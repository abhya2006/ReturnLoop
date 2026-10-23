package com.returnloop.identity.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.returnloop.common.error.ApiException;
import com.returnloop.common.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Validates Google ID tokens using Google's published signing keys. */
@Component
public class GoogleTokenVerifier {
    private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Pattern MAX_AGE = Pattern.compile("(?:^|,)\\s*max-age=(\\d+)");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String clientId;
    private final RestClient restClient = RestClient.create();
    private volatile KeyCache keyCache;

    public GoogleTokenVerifier(@Value("${returnloop.auth.google.client-id:}") String clientId) {
        this.clientId = clientId;
    }

    public GoogleIdentity verify(String credential) {
        if (clientId.isBlank() || credential == null || credential.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Google sign-in is not configured");
        }
        try {
            String[] tokenParts = credential.split("\\.");
            if (tokenParts.length != 3) throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
            Map<String, Object> header = JSON.readValue(Base64.getUrlDecoder().decode(tokenParts[0]),
                    new TypeReference<>() { });
            if (!"RS256".equals(header.get("alg"))) throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
            String keyId = String.valueOf(header.get("kid"));
            PublicKey publicKey = getKey(keyId);
            Jws<Claims> signed = Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(credential);
            Claims claims = signed.getPayload();
            String issuer = claims.getIssuer();
            String email = claims.get("email", String.class);
            Boolean emailVerified = claims.get("email_verified", Boolean.class);
            if (!("accounts.google.com".equals(issuer) || "https://accounts.google.com".equals(issuer))
                    || !claims.getAudience().contains(clientId)
                    || claims.getExpiration() == null || !claims.getExpiration().toInstant().isAfter(Instant.now())
                    || !Boolean.TRUE.equals(emailVerified) || email == null || email.isBlank()
                    || claims.getSubject() == null || claims.getSubject().isBlank()) {
                throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Google sign-in could not be verified");
            }
            String name = claims.get("name", String.class);
            String hostedDomain = claims.get("hd", String.class);
            return new GoogleIdentity(email.trim().toLowerCase(Locale.ROOT), name, claims.getSubject(), hostedDomain);
        } catch (ApiException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException | java.io.IOException exception) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Google sign-in could not be verified", exception);
        }
    }

    private PublicKey getKey(String keyId) {
        KeyCache cache = keyCache;
        if (cache == null || cache.expiresAt().isBefore(Instant.now()) || !cache.keys().containsKey(keyId)) {
            synchronized (this) {
                cache = keyCache;
                if (cache == null || cache.expiresAt().isBefore(Instant.now()) || !cache.keys().containsKey(keyId)) {
                    cache = loadKeys();
                    keyCache = cache;
                }
            }
        }
        PublicKey key = cache.keys().get(keyId);
        if (key == null) throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Google signing key was not recognised");
        return key;
    }

    private KeyCache loadKeys() {
        try {
            ResponseEntity<Map> response = restClient.get().uri(GOOGLE_JWKS_URI).retrieve().toEntity(Map.class);
            Object rawKeys = response.getBody() == null ? null : response.getBody().get("keys");
            if (!(rawKeys instanceof List<?> keys)) throw new IllegalStateException("Google returned no signing keys");
            Map<String, PublicKey> parsed = new java.util.HashMap<>();
            KeyFactory factory = KeyFactory.getInstance("RSA");
            for (Object rawKey : keys) {
                if (!(rawKey instanceof Map<?, ?> jwk) || !"RSA".equals(jwk.get("kty"))) continue;
                String kid = String.valueOf(jwk.get("kid"));
                byte[] modulus = Base64.getUrlDecoder().decode(String.valueOf(jwk.get("n")));
                byte[] exponent = Base64.getUrlDecoder().decode(String.valueOf(jwk.get("e")));
                parsed.put(kid, factory.generatePublic(new RSAPublicKeySpec(
                        new BigInteger(1, modulus), new BigInteger(1, exponent))));
            }
            String cacheControl = response.getHeaders().getCacheControl();
            Matcher maxAge = MAX_AGE.matcher(cacheControl == null ? "" : cacheControl);
            long cacheSeconds = maxAge.find() ? Long.parseLong(maxAge.group(1)) : 3600;
            return new KeyCache(Map.copyOf(parsed), Instant.now().plusSeconds(cacheSeconds));
        } catch (RestClientException exception) {
            throw new ApiException(ErrorCode.IDENTITY_PROVIDER_UNAVAILABLE,
                    ErrorCode.IDENTITY_PROVIDER_UNAVAILABLE.getDefaultMessage(), exception);
        } catch (Exception exception) {
            if (exception instanceof ApiException apiException) throw apiException;
            throw new ApiException(ErrorCode.IDENTITY_PROVIDER_UNAVAILABLE,
                    ErrorCode.IDENTITY_PROVIDER_UNAVAILABLE.getDefaultMessage(), exception);
        }
    }

    private record KeyCache(Map<String, PublicKey> keys, Instant expiresAt) { }
    public record GoogleIdentity(String email, String name, String subject, String hostedDomain) { }
}
