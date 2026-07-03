package com.ascentium.kyc.repository;

import com.ascentium.kyc.entity.UserMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserMappingRepository extends JpaRepository<UserMapping, Long> {

    Optional<UserMapping> findByClientId(Long clientId);

    boolean existsByClientId(Long clientId);

    List<UserMapping> findByReviewerId(Long reviewerId);

    List<UserMapping> findByComplianceOfficerId(Long complianceOfficerId);
}
