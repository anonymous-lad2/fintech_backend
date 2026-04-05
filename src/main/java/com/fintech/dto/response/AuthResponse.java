package com.fintech.dto.response;

import com.fintech.entity.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType;    // Always Bearer
    private long expiresIn;      // Seconds
    private String email;
    private Role role;
}
