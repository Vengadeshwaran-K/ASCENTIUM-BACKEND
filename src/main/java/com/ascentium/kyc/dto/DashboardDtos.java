package com.ascentium.kyc.dto;

import com.ascentium.kyc.dto.KycDtos.KycResponse;
import com.ascentium.kyc.entity.Role;

import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record DashboardSection(long count, List<KycResponse> items) {
        public static DashboardSection of(List<KycResponse> items) {
            return new DashboardSection(items.size(), items);
        }
    }

    /** {@code role} names which role's scoped view this payload contains. */
    public record DashboardResponse(
            Role role,
            DashboardSection pending,
            DashboardSection awaitingClientDocuments,
            DashboardSection approved,
            DashboardSection rejected) {
    }
}
