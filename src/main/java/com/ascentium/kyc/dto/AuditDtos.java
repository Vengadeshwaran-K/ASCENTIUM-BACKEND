package com.ascentium.kyc.dto;

import com.ascentium.kyc.entity.AuditAction;
import com.ascentium.kyc.entity.AuditLog;
import com.ascentium.kyc.entity.RiskTier;
import com.ascentium.kyc.entity.Role;

import java.time.Instant;

public final class AuditDtos {

    private AuditDtos() {
    }

    public record AuditLogResponse(
            Long id,
            Long kycRequestId,
            AuditAction action,
            Long actorId,
            String actorName,
            Role actorRole,
            String remarks,
            RiskTier previousRiskTier,
            RiskTier newRiskTier,
            Instant createdAt) {

        public static AuditLogResponse from(AuditLog log) {
            return new AuditLogResponse(
                    log.getId(),
                    log.getKycRequest().getId(),
                    log.getAction(),
                    log.getActor().getId(),
                    log.getActor().getFullName(),
                    log.getActorRole(),
                    log.getRemarks(),
                    log.getPreviousRiskTier(),
                    log.getNewRiskTier(),
                    log.getCreatedAt());
        }
    }
}
