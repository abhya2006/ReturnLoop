package com.returnloop.notification;

import com.returnloop.common.error.ApiException;
import com.returnloop.common.web.PageResponse;
import com.returnloop.identity.user.UserAccount;
import com.returnloop.identity.user.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Creates and reads in-app notifications.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final UserAccountRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID userId, Pageable pageable) {
        Page<Notification> page = repository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.from(page, NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void notifyOrganization(UUID organizationId, String title, String body, String category,
                                   String severity, String link, String referenceType, UUID referenceId) {
        List<UserAccount> recipients = userRepository.findByOrganizationId(organizationId);
        for (UserAccount recipient : recipients) {
            if (!recipient.isActive()) {
                continue;
            }
            Notification notification = new Notification();
            notification.setOrganizationId(organizationId);
            notification.setUserId(recipient.getId());
            notification.setTitle(title);
            notification.setBody(body);
            notification.setCategory(category);
            notification.setSeverity(severity);
            notification.setLink(link);
            notification.setReferenceType(referenceType);
            notification.setReferenceId(referenceId);
            repository.save(notification);
        }
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound("Notification", notificationId));
        if (!notification.getUserId().equals(userId)) {
            throw new com.returnloop.common.error.ApiException(
                    com.returnloop.common.error.ErrorCode.CROSS_TENANT_ACCESS);
        }
        notification.setRead(true);
        notification.setReadAt(java.time.Instant.now());
        return NotificationResponse.from(repository.save(notification));
    }

    @Transactional
    public long markAllRead(UUID userId) {
        return repository.markAllRead(userId);
    }
}
