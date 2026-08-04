package com.devsquad.notification.application.port;

import com.devsquad.notification.application.NotificationService.NotificationView;
import java.util.List;
import java.util.UUID;

public interface NotificationStore {
    List<NotificationView> findByReceiver(UUID receiverId);
    void markRead(UUID notificationId, UUID receiverId);
}
