package com.ascentium.kyc.dto;

import com.ascentium.kyc.entity.BeneficialOwner;
import com.ascentium.kyc.entity.KycRequest;
import com.ascentium.kyc.entity.KycStatus;
import com.ascentium.kyc.entity.KycType;
import com.ascentium.kyc.entity.RiskTier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class KycDtos {

    private KycDtos() {
    }

    public record BeneficialOwnerDto(
            @NotBlank String fullName,
            @Past LocalDate dateOfBirth,
            String nationality,
            String idNumber,
            Double ownershipPercentage) {

        public static BeneficialOwnerDto from(BeneficialOwner owner) {
            return new BeneficialOwnerDto(owner.getFullName(), owner.getDateOfBirth(),
                    owner.getNationality(), owner.getIdNumber(), owner.getOwnershipPercentage());
        }
    }

    public record KycUpsertRequest(
            @NotNull KycType type,
            // Individual
            String fullLegalName,
            @Past LocalDate dateOfBirth,
            String nationality,
            String countryOfResidence,
            String governmentIdNumber,
            String residentialAddress,
            // Business / Entity
            String legalEntityName,
            String registrationNumber,
            @Past LocalDate dateOfIncorporation,
            String registeredBusinessAddress,
            @Valid List<BeneficialOwnerDto> beneficialOwners) {
    }

    public record ApproveRequest(String comment) {
    }

    public record RejectRequest(
            @NotBlank(message = "A valid reason is required when rejecting a request") String reason) {
    }

    public record RiskTierRequest(@NotNull RiskTier riskTier) {
    }

    public record KycResponse(
            Long id,
            Long clientId,
            KycType type,
            // Individual
            String fullLegalName,
            LocalDate dateOfBirth,
            String nationality,
            String countryOfResidence,
            String governmentIdNumber,
            String residentialAddress,
            // Business / Entity
            String legalEntityName,
            String registrationNumber,
            LocalDate dateOfIncorporation,
            String registeredBusinessAddress,
            List<BeneficialOwnerDto> beneficialOwners,
            // Workflow
            KycStatus status,
            Integer formVersion,
            String rejectedBy,
            String rejectionReason,
            Instant submittedAt,
            String reviewerComment,
            Instant reviewedAt,
            String complianceComment,
            Instant decidedAt,
            RiskTier riskTier,
            Instant riskTierUpdatedAt,
            Instant createdAt) {

        public static KycResponse from(KycRequest kyc) {
            return new KycResponse(
                    kyc.getId(),
                    kyc.getClient().getId(),
                    kyc.getType(),
                    kyc.getFullLegalName(),
                    kyc.getDateOfBirth(),
                    kyc.getNationality(),
                    kyc.getCountryOfResidence(),
                    kyc.getGovernmentIdNumber(),
                    kyc.getResidentialAddress(),
                    kyc.getLegalEntityName(),
                    kyc.getRegistrationNumber(),
                    kyc.getDateOfIncorporation(),
                    kyc.getRegisteredBusinessAddress(),
                    kyc.getBeneficialOwners().stream().map(BeneficialOwnerDto::from).toList(),
                    kyc.getStatus(),
                    kyc.getFormVersion(),
                    kyc.getRejectedBy() != null ? kyc.getRejectedBy().name() : null,
                    kyc.getRejectionReason(),
                    kyc.getSubmittedAt(),
                    kyc.getReviewerComment(),
                    kyc.getReviewedAt(),
                    kyc.getComplianceComment(),
                    kyc.getDecidedAt(),
                    kyc.getRiskTier(),
                    kyc.getRiskTierUpdatedAt(),
                    kyc.getCreatedAt());
        }
    }
}
