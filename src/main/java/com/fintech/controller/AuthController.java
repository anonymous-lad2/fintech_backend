package com.fintech.controller;

import com.fintech.config.RateLimiterService;
import com.fintech.dto.request.LoginRequest;
import com.fintech.dto.response.AuthResponse;
import com.fintech.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and token management")
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate and recieve a JWT access token")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest
    ) {
        String clientIp = resolveClientIp(httpRequest);

        if(!rateLimiterService.resolveLoginBucket(clientIp).tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "60")
                    .body("Too many login attempts. Please try again in 60 seconds");
        }

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    private String resolveClientIp(HttpServletRequest httpRequest) {

        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        if(forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return httpRequest.getRemoteAddr();
    }
}
