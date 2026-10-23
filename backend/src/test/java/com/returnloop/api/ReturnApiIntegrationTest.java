package com.returnloop.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.returnloop.catalog.Product;
import com.returnloop.catalog.ProductCategory;
import com.returnloop.catalog.ProductRepository;
import com.returnloop.identity.user.UserAccount;
import com.returnloop.identity.user.UserAccountRepository;
import com.returnloop.organization.Organization;
import com.returnloop.organization.OrganizationRepository;
import com.returnloop.security.JwtService;
import com.returnloop.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of the return lifecycle over the real HTTP stack:
 * authentication, tenant scoping, creation, illegal transitions and the
 * recovery engine.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReturnApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JwtService jwtService;

    private UUID organizationId;
    private String token;
    private String sku;

    @BeforeEach
    void setUp() {
        Organization organization = organizationRepository
                .findBySlug("it-organization")
                .orElseGet(() -> {
                    Organization created = new Organization();
                    created.setName("Integration Test Org");
                    created.setSlug("it-organization");
                    return organizationRepository.save(created);
                });
        organizationId = organization.getId();

        UserAccount user = userRepository
                .findByEmailIgnoreCase("it-admin@example.com")
                .orElseGet(() -> {
                    UserAccount created = new UserAccount();
                    created.setOrganizationId(organizationId);
                    created.setEmail("it-admin@example.com");
                    created.setFullName("Integration Admin");
                    created.setRole(Role.ADMIN);
                    created.setActive(true);
                    created.setPasswordHash("{noop}unused");
                    return userRepository.save(created);
                });
        token = jwtService.issue(user, organizationId);

        sku = "IT-SKU-" + UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product();
        product.setOrganizationId(organizationId);
        product.setSku(sku);
        product.setName("Integration Test Product");
        product.setCategory(ProductCategory.ELECTRONICS);
        product.setOriginalPrice(new BigDecimal("900.00"));
        product.setResaleValue(new BigDecimal("600.00"));
        product.setWeightKg(new BigDecimal("1.50"));
        productRepository.save(product);
    }

    private String bearer() {
        return "Bearer " + token;
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("an unauthenticated request is rejected")
    void unauthenticatedIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/returns"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("a return can be created, fetched and moved through the lifecycle")
    void createAndProgress() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/returns")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "DEFECTIVE",
                                  "customerName": "Jordan Alvarez",
                                  "sourceReference": "ORD-1",
                                  "items": [
                                    {"sku": "%s", "quantity": 2, "condition": "GOOD"}
                                  ]
                                }
                                """.formatted(sku)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("RETURN_CREATED"))
                .andExpect(jsonPath("$.data.itemCount").value(2))
                .andExpect(jsonPath("$.data.returnNumber").exists())
                .andReturn();

        String returnId = body(created).path("data").path("id").asText();
        String returnNumber = body(created).path("data").path("returnNumber").asText();
        assertThat(returnNumber).startsWith("RET-");

        mockMvc.perform(get("/api/v1/returns/{id}", returnId).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.timeline.length()").value(1));

        mockMvc.perform(post("/api/v1/returns/{id}/status", returnId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PICKUP_SCHEDULED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PICKUP_SCHEDULED"));
    }

    @Test
    @DisplayName("an illegal lifecycle transition is rejected with a clear message")
    void illegalTransitionIsRejected() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/returns")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"DEFECTIVE","items":[{"sku":"%s","quantity":1}]}
                                """.formatted(sku)))
                .andExpect(status().isCreated())
                .andReturn();
        String returnId = body(created).path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/returns/{id}/status", returnId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    @DisplayName("a cancellation without a reason is rejected")
    void cancellationRequiresAReason() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/returns")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"DEFECTIVE","items":[{"sku":"%s","quantity":1}]}
                                """.formatted(sku)))
                .andExpect(status().isCreated())
                .andReturn();
        String returnId = body(created).path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/returns/{id}/cancel", returnId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("an unknown SKU is reported rather than silently ignored")
    void unknownSkuIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/returns")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"DEFECTIVE","items":[{"sku":"NO-SUCH-SKU","quantity":1}]}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("the recovery engine returns ranked options with their economics")
    void recoveryOptionsAreReturned() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/returns")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"DEFECTIVE","items":[{"sku":"%s","quantity":1,"condition":"GOOD"}]}
                                """.formatted(sku)))
                .andExpect(status().isCreated())
                .andReturn();
        String returnId = body(created).path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/recovery/returns/{id}/options", returnId).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].options.length()").value(8))
                .andExpect(jsonPath("$.data.items[0].recommended.path").exists())
                .andExpect(jsonPath("$.data.strategy").value("rule-based-explainable"));
    }

    @Test
    @DisplayName("the dashboard aggregates real records")
    void dashboardAggregates() throws Exception {
        mockMvc.perform(post("/api/v1/returns")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"OVERPURCHASE","items":[{"sku":"%s","quantity":3}]}
                                """.formatted(sku)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/analytics/dashboard").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReturns.label").value("Total Returns"))
                .andExpect(jsonPath("$.data.trend").isArray());
    }

    @Test
    @DisplayName("another organisation's return is not reachable")
    void crossTenantAccessIsRejected() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/returns")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"DEFECTIVE","items":[{"sku":"%s","quantity":1}]}
                                """.formatted(sku)))
                .andExpect(status().isCreated())
                .andReturn();
        String returnId = body(created).path("data").path("id").asText();

        Organization other = new Organization();
        other.setName("Other Org");
        other.setSlug("other-org-" + UUID.randomUUID().toString().substring(0, 6));
        other = organizationRepository.save(other);

        UserAccount otherUser = new UserAccount();
        otherUser.setOrganizationId(other.getId());
        otherUser.setEmail("other-" + UUID.randomUUID().toString().substring(0, 6) + "@example.com");
        otherUser.setFullName("Other User");
        otherUser.setRole(Role.VIEWER);
        otherUser.setActive(true);
        otherUser.setPasswordHash("{noop}unused");
        otherUser = userRepository.save(otherUser);
        String otherToken = jwtService.issue(otherUser, other.getId());

        mockMvc.perform(get("/api/v1/returns/{id}", returnId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CROSS_TENANT_ACCESS"));
    }
}
