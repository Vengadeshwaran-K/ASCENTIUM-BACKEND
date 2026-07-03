package com.ascentium.kyc.controller;

import com.ascentium.kyc.dto.DashboardDtos.DashboardResponse;
import com.ascentium.kyc.dto.KycDtos.KycResponse;
import com.ascentium.kyc.dto.KycDtos.KycUpsertRequest;
import com.ascentium.kyc.security.AppUserPrincipal;
import com.ascentium.kyc.service.KycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kyc")
@PreAuthorize("hasRole('CLIENT')")
@RequiredArgsConstructor
public class ClientKycController {

    private final KycService kycService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> dashboard(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(kycService.getClientDashboard(principal.getUser()));
    }

    @PostMapping
    public ResponseEntity<KycResponse> createDraft(@AuthenticationPrincipal AppUserPrincipal principal,
                                                   @Valid @RequestBody KycUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(kycService.createDraft(principal.getUser(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<KycResponse> updateDraft(@AuthenticationPrincipal AppUserPrincipal principal,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody KycUpsertRequest request) {
        return ResponseEntity.ok(kycService.updateDraft(principal.getUser(), id, request));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<KycResponse> submit(@AuthenticationPrincipal AppUserPrincipal principal,
                                              @PathVariable Long id) {
        return ResponseEntity.ok(kycService.submit(principal.getUser(), id));
    }

    @GetMapping("/my")
    public ResponseEntity<List<KycResponse>> myRequests(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(kycService.getMyRequests(principal.getUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<KycResponse> getOne(@AuthenticationPrincipal AppUserPrincipal principal,
                                              @PathVariable Long id) {
        return ResponseEntity.ok(kycService.getMyRequest(principal.getUser(), id));
    }
}
