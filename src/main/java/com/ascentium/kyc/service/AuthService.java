package com.ascentium.kyc.service;

import com.ascentium.kyc.dto.AuthDtos.AuthResponse;
import com.ascentium.kyc.dto.AuthDtos.LoginRequest;
import com.ascentium.kyc.dto.AuthDtos.RegisterRequest;
import com.ascentium.kyc.entity.Role;
import com.ascentium.kyc.entity.User;
import com.ascentium.kyc.exception.BusinessException;
import com.ascentium.kyc.repository.UserRepository;
import com.ascentium.kyc.security.AppUserPrincipal;
import com.ascentium.kyc.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

  public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered");
        }
        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.CLIENT)
                .build();
        userRepository.save(user);
        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = ((AppUserPrincipal) authentication.getPrincipal()).getUser();
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}