package com.ascentium.kyc.dto;

import com.ascentium.kyc.entity.Role;
import com.ascentium.kyc.entity.User;

import java.time.Instant;

public record UserResponse(Long id, String fullName, String email, Role role, boolean active, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt());
    }
}