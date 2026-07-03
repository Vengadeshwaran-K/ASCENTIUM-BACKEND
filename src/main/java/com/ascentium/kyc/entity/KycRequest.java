package com.ascentium.kyc.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "kyc_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private User client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycType type;

    // --- Individual fields (mandatory at submit when type = INDIVIDUAL) ---
    private String fullLegalName;

    private LocalDate dateOfBirth;

    private String nationality;

    private String countryOfResidence;

    private String governmentIdNumber;

    private String residentialAddress;

    // --- Business / Entity fields (mandatory at submit when type = BUSINESS) ---
    private String legalEntityName;

    private String registrationNumber;

    private LocalDate dateOfIncorporation;

    private String registeredBusinessAddress;

    @OneToMany(mappedBy = "kycRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BeneficialOwner> beneficialOwners = new ArrayList<>();

    // --- Workflow state ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private KycStatus status = KycStatus.DRAFT;

   @Column(nullable = false)
    @Builder.Default
    private Integer formVersion = 1;

  @Enumerated(EnumType.STRING)
    private Role rejectedBy;

 private String rejectionReason;

    private Instant submittedAt;

   private String reviewerComment;

    private Instant reviewedAt;

    private String complianceComment;

    private Instant decidedAt;

    @Enumerated(EnumType.STRING)
    private RiskTier riskTier;

    private Instant riskTierUpdatedAt;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean isEditable() {
        return status == KycStatus.DRAFT || status == KycStatus.RESUBMISSION_REQUIRED;
    }
}