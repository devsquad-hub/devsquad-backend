package com.devsquad.notification.adapter.out.persistence;

import com.devsquad.notification.application.NotificationService.NotificationView;
import com.devsquad.notification.application.port.NotificationStore;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcNotificationStore implements NotificationStore {
    private final JdbcClient jdbc;

    public JdbcNotificationStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<NotificationView> findByReceiver(UUID receiverId) {
        return jdbc.sql("""
                        select id, type, title, body, project_id, entity_type, entity_id, created_at, read_at
                        from notifications where receiver_id = :accountId order by created_at desc limit 100
                        """).param("accountId", receiverId)
                .query((rs, row) -> new NotificationView(rs.getObject("id", UUID.class), rs.getString("type"),
                        rs.getString("title"), rs.getString("body"), rs.getObject("project_id", UUID.class),
                        rs.getString("entity_type"), rs.getObject("entity_id", UUID.class),
                        rs.getObject("created_at", OffsetDateTime.class), rs.getObject("read_at", OffsetDateTime.class)))
                .list();
    }

    @Override
    public void markRead(UUID notificationId, UUID receiverId) {
        jdbc.sql("update notifications set read_at = coalesce(read_at, now()) where id = :id and receiver_id = :account")
                .param("id", notificationId).param("account", receiverId).update();
    }
}
