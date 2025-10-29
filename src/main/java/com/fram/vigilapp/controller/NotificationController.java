package com.fram.vigilapp.controller;

import com.fram.vigilapp.dto.NotificationDto;
import com.fram.vigilapp.service.NotificationService;
import com.fram.vigilapp.util.JwtUtil;
import com.fram.vigilapp.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;
    private final UserUtil userUtil;

    /**
     * Get paginated notifications for the current user
     * GET /api/notifications/me?page=0&size=20
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<Page<NotificationDto>> getUserNotifications(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = userUtil.getUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationDto> notifications = notificationService.getUserNotifications(userId, pageable);

        return ResponseEntity.ok(notifications);
    }

    /**
     * Get single notification by ID
     * GET /api/notifications/{notificationId}
     */
    @GetMapping("/{notificationId}")
    @PreAuthorize("hasAnyRole('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<NotificationDto> getNotification(
            @PathVariable UUID notificationId) {

        NotificationDto notification = notificationService.getNotificationById(notificationId);
        return ResponseEntity.ok(notification);
    }

    /**
     * Mark notification as delivered
     * PUT /api/notifications/{notificationId}/delivered
     */
    @PutMapping("/{notificationId}/delivered")
    @PreAuthorize("hasAnyRole('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<NotificationDto> markAsDelivered(
            @PathVariable UUID notificationId) {

        NotificationDto notification = notificationService.markAsDelivered(notificationId);
        return ResponseEntity.ok(notification);
    }

    /**
     * Delete a notification
     * DELETE /api/notifications/{notificationId}
     */
    @DeleteMapping("/{notificationId}")
    @PreAuthorize("hasAnyRole('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID notificationId) {

        notificationService.deleteNotification(notificationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get count of undelivered notifications
     * GET /api/notifications/undelivered/count
     */
    @GetMapping("/undelivered/count")
    @PreAuthorize("hasAnyRole('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<Long> countUndeliveredNotifications(
            @RequestHeader(value = "Authorization", required = false) String token) {

        UUID userId = userUtil.getUserId();
        long count = notificationService.countUndeliveredNotifications(userId);

        return ResponseEntity.ok(count);
    }

    /**
     * Get queued notifications (admin/mod only - for processing)
     * GET /api/notifications/queued
     */
    @GetMapping("/queued")
    @PreAuthorize("hasAnyRole('MOD', 'ADMIN')")
    public ResponseEntity<java.util.List<NotificationDto>> getQueuedNotifications() {
        return ResponseEntity.ok(notificationService.getQueuedNotifications());
    }

    /**
     * Get queued notifications by channel (admin/mod only)
     * GET /api/notifications/queued?channel=PUSH
     */
    @GetMapping("/queued/by-channel")
    @PreAuthorize("hasAnyRole('MOD', 'ADMIN')")
    public ResponseEntity<java.util.List<NotificationDto>> getQueuedNotificationsByChannel(
            @RequestParam String channel) {
        return ResponseEntity.ok(notificationService.getQueuedNotificationsByChannel(channel));
    }
}
