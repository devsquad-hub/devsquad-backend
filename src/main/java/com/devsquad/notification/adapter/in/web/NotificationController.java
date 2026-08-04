package com.devsquad.notification.adapter.in.web;

import static com.devsquad.shared.security.JwtSubject.subject;
import com.devsquad.notification.application.NotificationService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificationService.NotificationView> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(subject(jwt));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> read(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID notificationId) {
        service.markRead(subject(jwt), notificationId);
        return ResponseEntity.noContent().build();
    }
}
