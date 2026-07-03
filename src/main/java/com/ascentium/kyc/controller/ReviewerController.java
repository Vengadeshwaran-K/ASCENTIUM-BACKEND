package com.ascentium.kyc.controller;

import com.ascentium.kyc.dto.DashboardDtos.DashboardResponse;
import com.ascentium.kyc.dto.KycDtos.ApproveRequest;
import com.ascentium.kyc.dto.KycDtos.KycResponse;
import com.ascentium.kyc.dto.KycDtos.RejectRequest;
import com.ascentium.kyc.security.AppUserPrincipal;
import com.ascentium.kyc.service.KycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review")
@PreAuthorize("hasRole('REVIEWER')")
@RequiredArgsConstructor
public class ReviewerController {

    private final KycService kycService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> dashboard(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(kycService.getReviewerDashboard(principal.getUser()));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<KycResponse>> pending(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(kycService.getPendingReview(principal.getUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<KycResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(kycService.getById(id));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<KycResponse> accept(@AuthenticationPrincipal AppUserPrincipal principal,
                                              @PathVariable Long id,
                                              @RequestBody(required = false) ApproveRequest request) {
        String comment = request != null ? request.comment() : null;
        return ResponseEntity.ok(kycService.approveReview(principal.getUser(), id, comment));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<KycResponse> reject(@AuthenticationPrincipal AppUserPrincipal principal,
                                              @PathVariable Long id,
                                              @Valid @RequestBody RejectRequest request) {
        return ResponseEntity.ok(kycService.rejectReview(principal.getUser(), id, request.reason()));
    }
}
