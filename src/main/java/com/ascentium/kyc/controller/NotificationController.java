package com.ascentium.kyc.controller;

import com.ascentium.kyc.dto.NotificationDtos.NotificationResponse;
import com.ascentium.kyc.dto.NotificationDtos.UnreadCountResponse;
import com.ascentium.kyc.security.AppUserPrincipal;
import com.ascentium.kyc.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Every role reads this the same way — only their own notifications, scoped by recipient, never by role. */
@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'REVIEWER', 'CLIENT')")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMine(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getMyNotifications(principal.getUser()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(new UnreadCountResponse(notificationService.getUnreadCount(principal.getUser())));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@AuthenticationPrincipal AppUserPrincipal principal,
                                                         @PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markRead(principal.getUser(), id));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal AppUserPrincipal principal) {
        notificationService.markAllRead(principal.getUser());
        return ResponseEntity.noContent().build();
    }
}
