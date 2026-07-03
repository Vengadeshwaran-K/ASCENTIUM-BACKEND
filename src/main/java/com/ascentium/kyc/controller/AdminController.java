package com.ascentium.kyc.controller;

import com.ascentium.kyc.dto.AuditDtos.AuditLogResponse;
import com.ascentium.kyc.dto.AuthDtos.CreateUserRequest;
import com.ascentium.kyc.dto.DashboardDtos.DashboardResponse;
import com.ascentium.kyc.dto.KycDtos.KycResponse;
import com.ascentium.kyc.dto.MappingDtos.UserMappingRequest;
import com.ascentium.kyc.dto.MappingDtos.UserMappingResponse;
import com.ascentium.kyc.dto.UserResponse;
import com.ascentium.kyc.entity.Role;
import com.ascentium.kyc.security.AppUserPrincipal;
import com.ascentium.kyc.service.KycService;
import com.ascentium.kyc.service.UserMappingService;
import com.ascentium.kyc.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final KycService kycService;
    private final UserMappingService userMappingService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> dashboard(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(kycService.getAdminDashboard(principal.getUser()));
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getUsers(@RequestParam(required = false) Role role) {
        return ResponseEntity.ok(role == null ? userService.getAllUsers() : userService.getUsersByRole(role));
    }

    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<UserResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(userService.setActive(id, true));
    }

    @PatchMapping("/users/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(userService.setActive(id, false));
    }

    @GetMapping("/kyc")
    public ResponseEntity<List<KycResponse>> getAllKyc() {
        return ResponseEntity.ok(kycService.getAll());
    }

    @GetMapping("/kyc/{id}/audit")
    public ResponseEntity<List<AuditLogResponse>> auditTrail(@PathVariable Long id) {
        return ResponseEntity.ok(kycService.getAuditTrailAsAdmin(id));
    }


    @PostMapping("/mappings")
    public ResponseEntity<UserMappingResponse> createMapping(@Valid @RequestBody UserMappingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userMappingService.createMapping(request));
    }

    @PutMapping("/mappings/{id}")
    public ResponseEntity<UserMappingResponse> updateMapping(@PathVariable Long id,
                                                             @Valid @RequestBody UserMappingRequest request) {
        return ResponseEntity.ok(userMappingService.updateMapping(id, request));
    }

    @GetMapping("/mappings")
    public ResponseEntity<List<UserMappingResponse>> getMappings() {
        return ResponseEntity.ok(userMappingService.getAllMappings());
    }

    @GetMapping("/mappings/client/{clientId}")
    public ResponseEntity<UserMappingResponse> getMappingByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(userMappingService.getMappingByClient(clientId));
    }

    @DeleteMapping("/mappings/{id}")
    public ResponseEntity<Void> deleteMapping(@PathVariable Long id) {
        userMappingService.deleteMapping(id);
        return ResponseEntity.noContent().build();
    }
}
