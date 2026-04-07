package com.taskboard.controller;

import com.taskboard.model.dto.NotificationDTO;
import com.taskboard.model.entity.NotificationType;
import com.taskboard.security.UserPrincipal;
import com.taskboard.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Diagnostic: create a test notification directly (bypasses RabbitMQ).
     * Call: POST /api/v1/notifications/test
     */
    @PostMapping("/test")
    public ResponseEntity<NotificationDTO> sendTestNotification(
            @AuthenticationPrincipal UserPrincipal user) {
        log.warn(">>> TEST: Creating test notification for user {} (id={})", user.getUsername(), user.getId());
        NotificationDTO dto = notificationService.createNotification(
                user.getId(),
                NotificationType.CARD_ASSIGNED,
                "Test notification",
                "This is a test notification to verify the pipeline works.",
                null, null);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<Page<NotificationDTO>> getNotifications(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getNotifications(user.getId(), PageRequest.of(page, size)));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(user.getId()));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(user.getId())));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationDTO> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(notificationService.markAsRead(id, user.getId()));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(Map.of("updated", notificationService.markAllAsRead(user.getId())));
    }
}
