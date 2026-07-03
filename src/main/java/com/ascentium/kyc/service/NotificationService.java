package com.ascentium.kyc.service;

import com.ascentium.kyc.dto.NotificationDtos.NotificationResponse;
import com.ascentium.kyc.entity.KycRequest;
import com.ascentium.kyc.entity.Notification;
import com.ascentium.kyc.entity.NotificationType;
import com.ascentium.kyc.entity.User;
import com.ascentium.kyc.exception.NotFoundException;
import com.ascentium.kyc.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /** Fire-and-forget: called from other services at the point each event actually happens. */
    @Transactional
    public void notify(User recipient, NotificationType type, String message, KycRequest kycRequest) {
        notificationRepository.save(Notification.builder()
                .recipient(recipient)
                .type(type)
                .message(message)
                .kycRequest(kycRequest)
                .build());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(User user) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByRecipientIdAndReadFalse(user.getId());
    }

    @Transactional
    public NotificationResponse markRead(User user, Long id) {
        Notification n = notificationRepository.findByIdAndRecipientId(id, user.getId())
                .orElseThrow(() -> new NotFoundException("Notification not found: " + id));
        n.setRead(true);
        return NotificationResponse.from(notificationRepository.save(n));
    }

    @Transactional
    public void markAllRead(User user) {
        notificationRepository.markAllReadForRecipient(user.getId());
    }
}
