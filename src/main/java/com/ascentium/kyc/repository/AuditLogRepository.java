package com.ascentium.kyc.repository;

import com.ascentium.kyc.entity.AuditLog;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Deliberately extends the bare {@link Repository} marker instead of JpaRepository:
 * only insert and read are exposed, so no code path in the application can ever
 * update or delete an audit entry.
 */
public interface AuditLogRepository extends Repository<AuditLog, Long> {

    AuditLog save(AuditLog entry);

    List<AuditLog> findByKycRequestIdOrderByCreatedAtAsc(Long kycRequestId);
}
