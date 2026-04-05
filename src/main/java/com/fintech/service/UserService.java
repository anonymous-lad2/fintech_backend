package com.fintech.service;

import com.fintech.dto.request.CreateUserRequest;
import com.fintech.dto.request.UpdateUserRequest;
import com.fintech.dto.response.UserResponse;
import com.fintech.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);
    UserResponse getUserById(UUID id);
    Page<UserResponse> getAllUsers(Role role, Pageable pageable);
    UserResponse updateUser(UUID id, UpdateUserRequest request);
    void deactivateUser(UUID id);
}
