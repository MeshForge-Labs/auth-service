package com.booking.auth.service;

import com.booking.auth.dto.*;
import com.booking.auth.exception.BadCredentialsException;
import com.booking.auth.exception.ResourceConflictException;
import com.booking.auth.model.Role;
import com.booking.auth.model.User;
import com.booking.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration attempted for existing email: {}", request.getEmail());
            throw new ResourceConflictException("User already exists with this email");
        }
        String roleAuthority = "ADMIN".equalsIgnoreCase(request.getRole())
                ? Role.ADMIN.getAuthority()
                : Role.USER.getAuthority();
        User user = User.builder()
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(roleAuthority))
                .enabled(true)
                .build();
        user = userRepository.save(user);
        log.info("User registered successfully: userId={}, email={}", user.getId(), user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> {
                    log.warn("Login failed: unknown email {}", request.getEmail());
                    return new BadCredentialsException("Invalid email or password");
                });
        if (!user.isEnabled()) {
            log.warn("Login failed: disabled account {}", user.getEmail());
            throw new BadCredentialsException("Account is disabled");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: invalid password for email {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }
        log.info("User logged in successfully: userId={}", user.getId());
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public ValidateResponse validate(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            return ValidateResponse.builder().valid(false).build();
        }
        String token = bearerToken.substring(7);
        return jwtService.validateAndParse(token)
                .map(claims -> ValidateResponse.builder()
                        .valid(true)
                        .subject(claims.getSubject())
                        .roles(jwtService.getRolesFromClaims(claims))
                        .build())
                .orElse(ValidateResponse.builder().valid(false).build());
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRoles());
        long expiresInMs = jwtService.getExpirationMs();
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInMs(expiresInMs)
                .email(user.getEmail())
                .roles(user.getRoles().stream().collect(Collectors.toSet()))
                .build();
    }
}
