package com.ascentium.kyc.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ROLE_HEADER = "X-User-Role";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

   return request.getServletPath().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            reject(response, HttpStatus.UNAUTHORIZED, "Missing Authorization header (Bearer token required)");
            return;
        }

        String token = header.substring(7);
        String email = jwtService.extractSubject(token);
        if (email == null) {
            reject(response, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        String roleHeader = request.getHeader(ROLE_HEADER);
        if (roleHeader == null || roleHeader.isBlank()) {
            reject(response, HttpStatus.UNAUTHORIZED, "Missing " + ROLE_HEADER + " header");
            return;
        }

        AppUserPrincipal principal;
        try {
            principal = (AppUserPrincipal) userDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException e) {
            reject(response, HttpStatus.UNAUTHORIZED, "User no longer exists");
            return;
        }

        if (!principal.isEnabled()) {
            reject(response, HttpStatus.FORBIDDEN, "Account is deactivated");
            return;
        }

      String actualRole = principal.getUser().getRole().name();
        if (!actualRole.equalsIgnoreCase(roleHeader.trim())) {
            reject(response, HttpStatus.FORBIDDEN,
                    "Role header does not match the role assigned to this account");
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"timestamp":"%s","status":%d,"message":"%s"}"""
                .formatted(Instant.now(), status.value(), message));
    }
}