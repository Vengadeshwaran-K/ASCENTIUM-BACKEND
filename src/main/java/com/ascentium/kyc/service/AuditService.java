package com.ascentium.kyc.service;

import com.ascentium.kyc.dto.AuditDtos.AuditLogResponse;
import com.ascentium.kyc.entity.AuditAction;
import com.ascentium.kyc.entity.AuditLog;
import com.ascentium.kyc.entity.KycRequest;
import com.ascentium.kyc.entity.RiskTier;
import com.ascentium.kyc.entity.User;
import com.ascentium.kyc.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /** Runs inside the caller's transaction: the action and its audit entry commit or roll back together. */
    @Transactional
    public void record(KycRequest kycRequest, User actor, AuditAction action, String remarks) {
        record(kycRequest, actor, action, remarks, null, null);
    }

    @Transactional
    public void record(KycRequest kycRequest, User actor, AuditAction action, String remarks,
                       RiskTier previousRiskTier, RiskTier newRiskTier) {
        auditLogRepository.save(AuditLog.builder()
                .kycRequest(kycRequest)
                .actor(actor)
                .actorRole(actor.getRole())
                .action(action)
                .remarks(remarks)
                .previousRiskTier(previousRiskTier)
                .newRiskTier(newRiskTier)
                .build());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getTrail(Long kycRequestId) {
        return auditLogRepository.findByKycRequestIdOrderByCreatedAtAsc(kycRequestId).stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
