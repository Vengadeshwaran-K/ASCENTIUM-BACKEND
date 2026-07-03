package com.ascentium.kyc.service;

import com.ascentium.kyc.dto.DashboardDtos.DashboardResponse;
import com.ascentium.kyc.dto.DashboardDtos.DashboardSection;
import com.ascentium.kyc.dto.KycDtos.BeneficialOwnerDto;
import com.ascentium.kyc.dto.KycDtos.KycResponse;
import com.ascentium.kyc.dto.KycDtos.KycUpsertRequest;
import com.ascentium.kyc.entity.BeneficialOwner;
import com.ascentium.kyc.entity.KycRequest;
import com.ascentium.kyc.entity.KycStatus;
import com.ascentium.kyc.entity.KycType;
import com.ascentium.kyc.entity.Role;
import com.ascentium.kyc.entity.User;
import com.ascentium.kyc.entity.UserMapping;
import com.ascentium.kyc.exception.BusinessException;
import com.ascentium.kyc.exception.NotFoundException;
import com.ascentium.kyc.repository.KycRequestRepository;
import com.ascentium.kyc.repository.UserMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KycService {

    private static final List<KycStatus> OPEN_STATUSES = List.of(
            KycStatus.DRAFT, KycStatus.SUBMITTED, KycStatus.PENDING_COMPLIANCE,
            KycStatus.RETURNED_TO_REVIEWER, KycStatus.RESUBMISSION_REQUIRED);

    /** Statuses in which the reviewer has an action to take. */
    private static final List<KycStatus> REVIEWER_ACTIONABLE_STATUSES =
            List.of(KycStatus.SUBMITTED, KycStatus.RETURNED_TO_REVIEWER);

    private final KycRequestRepository kycRequestRepository;
    private final UserMappingRepository userMappingRepository;

    // ---------- Client: draft lifecycle ----------

    @Transactional
    public KycResponse createDraft(User client, KycUpsertRequest request) {
        if (kycRequestRepository.existsByClientIdAndStatusIn(client.getId(), OPEN_STATUSES)) {
            throw new BusinessException("You already have a KYC request in progress");
        }
        KycRequest kyc = KycRequest.builder()
                .client(client)
                .type(request.type())
                .status(KycStatus.DRAFT)
                .build();
        applyFields(kyc, request);
        return KycResponse.from(kycRequestRepository.save(kyc));
    }

    @Transactional
    public KycResponse updateDraft(User client, Long kycId, KycUpsertRequest request) {
        KycRequest kyc = getOwnKyc(client, kycId);
        requireEditable(kyc);
        kyc.setType(request.type());
        applyFields(kyc, request);
        return KycResponse.from(kycRequestRepository.save(kyc));
    }

    /**
     * Formally submits the request (first time) or resubmits it after a rejection.
     * Mandatory fields for the selected type are validated here; after this the
     * request is read-only for the client until the next rejection.
     */
    @Transactional
    public KycResponse submit(User client, Long kycId) {
        KycRequest kyc = getOwnKyc(client, kycId);
        requireEditable(kyc);

        List<String> missing = kyc.getType() == KycType.INDIVIDUAL
                ? validateIndividual(kyc)
                : validateBusiness(kyc);
        if (!missing.isEmpty()) {
            throw new BusinessException("Cannot submit, missing mandatory fields: "
                    + String.join(", ", missing));
        }
        if (!userMappingRepository.existsByClientId(client.getId())) {
            throw new BusinessException(
                    "No reviewer/compliance officer has been assigned to your account yet; contact the administrator");
        }

        // Resubmission after a rejection means the client fixed the form; bump the version.
        boolean isCorrection = kyc.getStatus() == KycStatus.RESUBMISSION_REQUIRED;
        if (isCorrection) {
            kyc.setFormVersion(kyc.getFormVersion() + 1);
        }
        kyc.setStatus(KycStatus.SUBMITTED);
        kyc.setSubmittedAt(Instant.now());
        kyc.setRejectedBy(null);
        kyc.setRejectionReason(null);
        kyc.setReviewerComment(null);
        kyc.setReviewedAt(null);
        kyc.setComplianceComment(null);
        kyc.setDecidedAt(null);
        return KycResponse.from(kycRequestRepository.save(kyc));
    }

    @Transactional(readOnly = true)
    public List<KycResponse> getMyRequests(User client) {
        return kycRequestRepository.findByClientId(client.getId()).stream()
                .map(KycResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public KycResponse getMyRequest(User client, Long kycId) {
        return KycResponse.from(getOwnKyc(client, kycId));
    }

    // ---------- Reviewer ----------

    /**
     * Requests whose client is mapped to this reviewer and are awaiting a reviewer
     * decision — either a fresh client submission, or one bounced back by compliance.
     */
    @Transactional(readOnly = true)
    public List<KycResponse> getPendingReview(User reviewer) {
        return kycRequestRepository.findByStatusInForReviewer(REVIEWER_ACTIONABLE_STATUSES, reviewer.getId()).stream()
                .map(KycResponse::from)
                .toList();
    }

    /** Accept: forward to the compliance officer. */
    @Transactional
    public KycResponse approveReview(User reviewer, Long kycId, String comment) {
        KycRequest kyc = requireReviewerAction(reviewer, kycId);
        kyc.setReviewerComment(comment);
        kyc.setReviewedAt(Instant.now());
        kyc.setStatus(KycStatus.PENDING_COMPLIANCE);
        kyc.setRejectedBy(null);
        kyc.setRejectionReason(null);
        return KycResponse.from(kycRequestRepository.save(kyc));
    }

    /** Reject: mandatory reason, form goes back to the client to fix and resubmit. */
    @Transactional
    public KycResponse rejectReview(User reviewer, Long kycId, String reason) {
        KycRequest kyc = requireReviewerAction(reviewer, kycId);
        kyc.setReviewerComment(reason);
        kyc.setReviewedAt(Instant.now());
        reject(kyc, Role.REVIEWER, KycStatus.RESUBMISSION_REQUIRED, reason);
        return KycResponse.from(kycRequestRepository.save(kyc));
    }

    // ---------- Compliance Officer ----------

    /** Only requests whose client is mapped to this compliance officer. */
    @Transactional(readOnly = true)
    public List<KycResponse> getPendingCompliance(User complianceOfficer) {
        return kycRequestRepository
                .findByStatusInForComplianceOfficer(List.of(KycStatus.PENDING_COMPLIANCE), complianceOfficer.getId()).stream()
                .map(KycResponse::from)
                .toList();
    }

    /** Accept: final approval, request is done. */
    @Transactional
    public KycResponse approveCompliance(User complianceOfficer, Long kycId, String comment) {
        KycRequest kyc = requireComplianceAction(complianceOfficer, kycId);
        kyc.setComplianceComment(comment);
        kyc.setDecidedAt(Instant.now());
        kyc.setStatus(KycStatus.APPROVED);
        kyc.setRejectedBy(null);
        kyc.setRejectionReason(null);
        return KycResponse.from(kycRequestRepository.save(kyc));
    }

    /** Reject: mandatory reason, form goes back to the reviewer (the previous stage) to re-decide. */
    @Transactional
    public KycResponse rejectCompliance(User complianceOfficer, Long kycId, String reason) {
        KycRequest kyc = requireComplianceAction(complianceOfficer, kycId);
        kyc.setComplianceComment(reason);
        kyc.setDecidedAt(Instant.now());
        reject(kyc, Role.COMPLIANCE_OFFICER, KycStatus.RETURNED_TO_REVIEWER, reason);
        return KycResponse.from(kycRequestRepository.save(kyc));
    }

    // ---------- Shared ----------

    @Transactional(readOnly = true)
    public List<KycResponse> getAll() {
        return kycRequestRepository.findAll().stream().map(KycResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public KycResponse getById(Long kycId) {
        return KycResponse.from(getKyc(kycId));
    }

    // ---------- Dashboard (one method per role, one endpoint per role) ----------

    /** CLIENT dashboard: only this client's own requests. */
    @Transactional(readOnly = true)
    public DashboardResponse getClientDashboard(User client) {
        List<KycRequest> mine = kycRequestRepository.findByClientId(client.getId());
        return buildDashboard(client.getRole(), mine,
                List.of(KycStatus.SUBMITTED, KycStatus.PENDING_COMPLIANCE, KycStatus.RETURNED_TO_REVIEWER),
                KycStatus.RESUBMISSION_REQUIRED,
                null);
    }

    /** REVIEWER dashboard: only requests whose client is mapped to this reviewer. */
    @Transactional(readOnly = true)
    public DashboardResponse getReviewerDashboard(User reviewer) {
        List<KycRequest> mapped = kycRequestRepository.findByStatusInForReviewer(
                List.of(KycStatus.SUBMITTED, KycStatus.PENDING_COMPLIANCE, KycStatus.RETURNED_TO_REVIEWER,
                        KycStatus.RESUBMISSION_REQUIRED, KycStatus.APPROVED),
                reviewer.getId());
        return buildDashboard(reviewer.getRole(), mapped,
                REVIEWER_ACTIONABLE_STATUSES,
                KycStatus.RESUBMISSION_REQUIRED,
                Role.REVIEWER);
    }

    /** COMPLIANCE_OFFICER dashboard: only requests whose client is mapped to this officer. */
    @Transactional(readOnly = true)
    public DashboardResponse getComplianceDashboard(User complianceOfficer) {
        List<KycRequest> mapped = kycRequestRepository.findByStatusInForComplianceOfficer(
                List.of(KycStatus.SUBMITTED, KycStatus.PENDING_COMPLIANCE, KycStatus.RETURNED_TO_REVIEWER,
                        KycStatus.RESUBMISSION_REQUIRED, KycStatus.APPROVED),
                complianceOfficer.getId());
        return buildDashboard(complianceOfficer.getRole(), mapped,
                List.of(KycStatus.PENDING_COMPLIANCE),
                KycStatus.RESUBMISSION_REQUIRED,
                Role.COMPLIANCE_OFFICER);
    }

    /** ADMIN dashboard: global view across every client. */
    @Transactional(readOnly = true)
    public DashboardResponse getAdminDashboard(User admin) {
        List<KycRequest> all = kycRequestRepository.findAll();
        return buildDashboard(admin.getRole(), all,
                List.of(KycStatus.SUBMITTED, KycStatus.PENDING_COMPLIANCE, KycStatus.RETURNED_TO_REVIEWER),
                KycStatus.RESUBMISSION_REQUIRED,
                null);
    }

    /**
     * Shared bucketing: pending = actionable-now statuses; awaitingClientDocuments = sent back
     * to the client for fixes; approved = terminal APPROVED; rejected = last rejected by
     * {@code rejectedByFilter} (or by anyone, when null — used for CLIENT/ADMIN views).
     */
    private DashboardResponse buildDashboard(Role role, List<KycRequest> scoped,
                                             List<KycStatus> pendingStatuses,
                                             KycStatus awaitingClientDocsStatus,
                                             Role rejectedByFilter) {
        List<KycResponse> pending = new ArrayList<>();
        List<KycResponse> awaitingClientDocuments = new ArrayList<>();
        List<KycResponse> approved = new ArrayList<>();
        List<KycResponse> rejected = new ArrayList<>();

        for (KycRequest kyc : scoped) {
            KycResponse dto = KycResponse.from(kyc);
            if (pendingStatuses.contains(kyc.getStatus())) {
                pending.add(dto);
            }
            if (kyc.getStatus() == awaitingClientDocsStatus) {
                awaitingClientDocuments.add(dto);
            }
            if (kyc.getStatus() == KycStatus.APPROVED) {
                approved.add(dto);
            }
            boolean wasRejected = kyc.getRejectedBy() != null
                    && (rejectedByFilter == null || kyc.getRejectedBy() == rejectedByFilter);
            if (wasRejected) {
                rejected.add(dto);
            }
        }

        return new DashboardResponse(role,
                DashboardSection.of(pending),
                DashboardSection.of(awaitingClientDocuments),
                DashboardSection.of(approved),
                DashboardSection.of(rejected));
    }

    // ---------- Internals ----------

    private void applyFields(KycRequest kyc, KycUpsertRequest request) {
        kyc.setFullLegalName(request.fullLegalName());
        kyc.setDateOfBirth(request.dateOfBirth());
        kyc.setNationality(request.nationality());
        kyc.setCountryOfResidence(request.countryOfResidence());
        kyc.setGovernmentIdNumber(request.governmentIdNumber());
        kyc.setResidentialAddress(request.residentialAddress());
        kyc.setLegalEntityName(request.legalEntityName());
        kyc.setRegistrationNumber(request.registrationNumber());
        kyc.setDateOfIncorporation(request.dateOfIncorporation());
        kyc.setRegisteredBusinessAddress(request.registeredBusinessAddress());

        kyc.getBeneficialOwners().clear();
        if (request.beneficialOwners() != null) {
            List<BeneficialOwner> owners = new ArrayList<>();
            for (BeneficialOwnerDto dto : request.beneficialOwners()) {
                owners.add(BeneficialOwner.builder()
                        .kycRequest(kyc)
                        .fullName(dto.fullName())
                        .dateOfBirth(dto.dateOfBirth())
                        .nationality(dto.nationality())
                        .idNumber(dto.idNumber())
                        .ownershipPercentage(dto.ownershipPercentage())
                        .build());
            }
            kyc.getBeneficialOwners().addAll(owners);
        }
    }

    private List<String> validateIndividual(KycRequest kyc) {
        List<String> missing = new ArrayList<>();
        requireText(missing, kyc.getFullLegalName(), "fullLegalName");
        if (kyc.getDateOfBirth() == null) missing.add("dateOfBirth");
        requireText(missing, kyc.getNationality(), "nationality");
        requireText(missing, kyc.getCountryOfResidence(), "countryOfResidence");
        requireText(missing, kyc.getGovernmentIdNumber(), "governmentIdNumber");
        requireText(missing, kyc.getResidentialAddress(), "residentialAddress");
        return missing;
    }

    private List<String> validateBusiness(KycRequest kyc) {
        List<String> missing = new ArrayList<>();
        requireText(missing, kyc.getLegalEntityName(), "legalEntityName");
        requireText(missing, kyc.getRegistrationNumber(), "registrationNumber");
        if (kyc.getDateOfIncorporation() == null) missing.add("dateOfIncorporation");
        requireText(missing, kyc.getRegisteredBusinessAddress(), "registeredBusinessAddress");
        if (kyc.getBeneficialOwners().isEmpty()) missing.add("beneficialOwners (at least one)");
        return missing;
    }

    private void requireText(List<String> missing, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            missing.add(fieldName);
        }
    }

    private void requireEditable(KycRequest kyc) {
        if (!kyc.isEditable()) {
            throw new BusinessException(
                    "Request is read-only after submission (status: " + kyc.getStatus() + ")");
        }
    }

    /** Shared precondition check for both approveReview and rejectReview. */
    private KycRequest requireReviewerAction(User reviewer, Long kycId) {
        KycRequest kyc = getKyc(kycId);
        if (!REVIEWER_ACTIONABLE_STATUSES.contains(kyc.getStatus())) {
            throw new BusinessException("Request is not awaiting review (status: " + kyc.getStatus() + ")");
        }
        UserMapping mapping = requireMapping(kyc);
        if (!mapping.getReviewer().getId().equals(reviewer.getId())) {
            throw new BusinessException("This request's client is not assigned to you");
        }
        return kyc;
    }

    /** Shared precondition check for both approveCompliance and rejectCompliance. */
    private KycRequest requireComplianceAction(User complianceOfficer, Long kycId) {
        KycRequest kyc = getKyc(kycId);
        if (kyc.getStatus() != KycStatus.PENDING_COMPLIANCE) {
            throw new BusinessException("Request is not awaiting compliance decision (status: " + kyc.getStatus() + ")");
        }
        UserMapping mapping = requireMapping(kyc);
        if (!mapping.getComplianceOfficer().getId().equals(complianceOfficer.getId())) {
            throw new BusinessException("This request's client is not assigned to you");
        }
        return kyc;
    }

    /** Rejection always needs a valid reason and sends the form back to the previous stage. */
    private void reject(KycRequest kyc, Role rejectedBy, KycStatus returnTo, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("A valid reason is required when rejecting a request");
        }
        kyc.setStatus(returnTo);
        kyc.setRejectedBy(rejectedBy);
        kyc.setRejectionReason(reason.trim());
    }

    private UserMapping requireMapping(KycRequest kyc) {
        return userMappingRepository.findByClientId(kyc.getClient().getId())
                .orElseThrow(() -> new BusinessException(
                        "No reviewer/compliance mapping exists for this request's client"));
    }

    private KycRequest getKyc(Long kycId) {
        return kycRequestRepository.findById(kycId)
                .orElseThrow(() -> new NotFoundException("KYC request not found: " + kycId));
    }

    private KycRequest getOwnKyc(User client, Long kycId) {
        KycRequest kyc = getKyc(kycId);
        if (!kyc.getClient().getId().equals(client.getId())) {
            throw new NotFoundException("KYC request not found: " + kycId);
        }
        return kyc;
    }
}
