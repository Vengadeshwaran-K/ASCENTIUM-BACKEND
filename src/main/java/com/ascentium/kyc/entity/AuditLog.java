package com.ascentium.kyc.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Append-only. Every column is updatable = false and the repository exposes no
 * update or delete operations; entries can only ever be inserted.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kyc_request_id", updatable = false)
    private KycRequest kycRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AuditAction action;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", updatable = false)
    private User actor;

    /** Snapshotted at write time so the trail stays truthful even if the user's role later changes. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Role actorRole;

    @Column(length = 1000, updatable = false)
    private String remarks;

    /** Only set for RISK_TIER_OVERRIDDEN: value before the override (null = not yet rated). */
    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private RiskTier previousRiskTier;

    /** Only set for RISK_TIER_OVERRIDDEN: value after the override. */
    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private RiskTier newRiskTier;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
