package com.ascentium.kyc.service;

import com.ascentium.kyc.dto.MappingDtos.UserMappingRequest;
import com.ascentium.kyc.dto.MappingDtos.UserMappingResponse;
import com.ascentium.kyc.entity.NotificationType;
import com.ascentium.kyc.entity.Role;
import com.ascentium.kyc.entity.User;
import com.ascentium.kyc.entity.UserMapping;
import com.ascentium.kyc.exception.BusinessException;
import com.ascentium.kyc.exception.NotFoundException;
import com.ascentium.kyc.repository.UserMappingRepository;
import com.ascentium.kyc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserMappingService {

    private final UserMappingRepository userMappingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public UserMappingResponse createMapping(UserMappingRequest request) {
        if (userMappingRepository.existsByClientId(request.clientId())) {
            throw new BusinessException("Client already has a mapping; update it instead");
        }
        return UserMappingResponse.from(userMappingRepository.save(
                UserMapping.builder()
                        .client(requireUser(request.clientId(), Role.CLIENT))
                        .reviewer(requireUser(request.reviewerId(), Role.REVIEWER))
                        .complianceOfficer(requireUser(request.complianceOfficerId(), Role.COMPLIANCE_OFFICER))
                        .build()));
    }

    @Transactional
    public UserMappingResponse updateMapping(Long mappingId, UserMappingRequest request) {
        UserMapping mapping = userMappingRepository.findById(mappingId)
                .orElseThrow(() -> new NotFoundException("Mapping not found: " + mappingId));
        User client = requireUser(request.clientId(), Role.CLIENT);
        if (!mapping.getClient().getId().equals(client.getId())
                && userMappingRepository.existsByClientId(client.getId())) {
            throw new BusinessException("Client already has a mapping; update it instead");
        }
        User previousReviewer = mapping.getReviewer();
        User newReviewer = requireUser(request.reviewerId(), Role.REVIEWER);

        mapping.setClient(client);
        mapping.setReviewer(newReviewer);
        mapping.setComplianceOfficer(requireUser(request.complianceOfficerId(), Role.COMPLIANCE_OFFICER));
        UserMapping saved = userMappingRepository.save(mapping);

        if (!previousReviewer.getId().equals(newReviewer.getId())) {
            notificationService.notify(newReviewer, NotificationType.REVIEWER_REASSIGNED,
                    "You have been assigned as reviewer for " + client.getFullName() + "'s KYC case.", null);
            notificationService.notify(client, NotificationType.REVIEWER_REASSIGNED,
                    "Your KYC case has been reassigned to a new reviewer.", null);
        }
        return UserMappingResponse.from(saved);
    }

    @Transactional
    public void deleteMapping(Long mappingId) {
        if (!userMappingRepository.existsById(mappingId)) {
            throw new NotFoundException("Mapping not found: " + mappingId);
        }
        userMappingRepository.deleteById(mappingId);
    }

    @Transactional(readOnly = true)
    public List<UserMappingResponse> getAllMappings() {
        return userMappingRepository.findAll().stream()
                .map(UserMappingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserMappingResponse getMappingByClient(Long clientId) {
        return userMappingRepository.findByClientId(clientId)
                .map(UserMappingResponse::from)
                .orElseThrow(() -> new NotFoundException("No mapping for client: " + clientId));
    }

    private User requireUser(Long userId, Role expectedRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        if (user.getRole() != expectedRole) {
            throw new BusinessException(
                    "User " + userId + " must have role " + expectedRole + " but has " + user.getRole());
        }
        if (!user.isActive()) {
            throw new BusinessException("User " + userId + " is deactivated");
        }
        return user;
    }
}
