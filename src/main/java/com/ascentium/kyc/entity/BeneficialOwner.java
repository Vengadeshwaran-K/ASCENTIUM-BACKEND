package com.ascentium.kyc.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "beneficial_owners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeneficialOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kyc_request_id")
    private KycRequest kycRequest;

    @Column(nullable = false)
    private String fullName;

    private LocalDate dateOfBirth;

    private String nationality;

    private String idNumber;

    private Double ownershipPercentage;
}