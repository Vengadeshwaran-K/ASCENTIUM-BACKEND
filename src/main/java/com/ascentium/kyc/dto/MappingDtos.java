package com.ascentium.kyc.dto;

import com.ascentium.kyc.entity.UserMapping;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class MappingDtos {

    private MappingDtos() {
    }

    public record UserMappingRequest(
            @NotNull Long clientId,
            @NotNull Long reviewerId,
            @NotNull Long complianceOfficerId) {
    }

    public record UserMappingResponse(
            Long id,
            UserResponse client,
            UserResponse reviewer,
            UserResponse complianceOfficer,
            Instant createdAt,
            Instant updatedAt) {

        public static UserMappingResponse from(UserMapping mapping) {
            return new UserMappingResponse(
                    mapping.getId(),
                    UserResponse.from(mapping.getClient()),
                    UserResponse.from(mapping.getReviewer()),
                    UserResponse.from(mapping.getComplianceOfficer()),
                    mapping.getCreatedAt(),
                    mapping.getUpdatedAt());
        }
    }
}
