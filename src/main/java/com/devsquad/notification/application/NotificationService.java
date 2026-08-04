package com.devsquad.notification.application;

import com.devsquad.notification.application.port.NotificationStore;
import com.devsquad.shared.security.AuthorizationService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationStore notifications;
    private final AuthorizationService authorization;

    public NotificationService(NotificationStore notifications, AuthorizationService authorization) {
        this.notifications = notifications;
        this.authorization = authorization;
    }

    public List<NotificationView> list(String clerkId) {
        return notifications.findByReceiver(authorization.requireAccount(clerkId));
    }

    @Transactional
    public void markRead(String clerkId, UUID notificationId) {
        notifications.markRead(notificationId, authorization.requireAccount(clerkId));
    }

    public record NotificationView(UUID id, String type, String title, String body, UUID projectId,
                                   String entityType, UUID entityId, OffsetDateTime createdAt, OffsetDateTime readAt) {}
}
