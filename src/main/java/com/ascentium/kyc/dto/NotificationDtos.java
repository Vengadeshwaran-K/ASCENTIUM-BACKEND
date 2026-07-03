package com.ascentium.kyc.dto;

import com.ascentium.kyc.entity.Notification;
import com.ascentium.kyc.entity.NotificationType;

import java.time.Instant;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(
            Long id,
            NotificationType type,
            String message,
            Long kycRequestId,
            boolean read,
            Instant createdAt) {

        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(
                    n.getId(),
                    n.getType(),
                    n.getMessage(),
                    n.getKycRequest() != null ? n.getKycRequest().getId() : null,
                    n.isRead(),
                    n.getCreatedAt());
        }
    }

    public record UnreadCountResponse(long count) {
    }
}
