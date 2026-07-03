package com.ascentium.kyc.repository;

import com.ascentium.kyc.entity.KycRequest;
import com.ascentium.kyc.entity.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KycRequestRepository extends JpaRepository<KycRequest, Long> {

    List<KycRequest> findByClientId(Long clientId);

    List<KycRequest> findByStatus(KycStatus status);

    List<KycRequest> findByStatusIn(List<KycStatus> statuses);

    boolean existsByClientIdAndStatusIn(Long clientId, List<KycStatus> statuses);

  @Query("""
            select k from KycRequest k
            where k.status in :statuses
              and k.client.id in (select m.client.id from UserMapping m where m.reviewer.id = :reviewerId)
            """)
    List<KycRequest> findByStatusInForReviewer(@Param("statuses") List<KycStatus> statuses,
                                               @Param("reviewerId") Long reviewerId);

  @Query("""
            select k from KycRequest k
            where k.status in :statuses
              and k.client.id in (select m.client.id from UserMapping m where m.complianceOfficer.id = :officerId)
            """)
    List<KycRequest> findByStatusInForComplianceOfficer(@Param("statuses") List<KycStatus> statuses,
                                                        @Param("officerId") Long officerId);
}
