package com.fintech.service.impl;

import com.fintech.config.JwtProperties;
import com.fintech.dto.request.LoginRequest;
import com.fintech.dto.response.AuthResponse;
import com.fintech.entity.User;
import com.fintech.repository.UserRepository;
import com.fintech.security.JwtTokenProvider;
import com.fintech.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    @Override
    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmailAndActiveTrue(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = tokenProvider.generateToken(user.getEmail(), user.getRole().name());
        log.info("User '{}' authenticated successfully", user.getEmail());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationSeconds())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
