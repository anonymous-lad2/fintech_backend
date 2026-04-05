package com.fintech.service;

import com.fintech.dto.request.LoginRequest;
import com.fintech.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
}
