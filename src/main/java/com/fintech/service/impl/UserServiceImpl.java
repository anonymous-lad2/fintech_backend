package com.fintech.service.impl;

import com.fintech.dto.request.CreateUserRequest;
import com.fintech.dto.request.UpdateUserRequest;
import com.fintech.dto.response.UserResponse;
import com.fintech.entity.Role;
import com.fintech.entity.User;
import com.fintech.exception.ResourceAlreadyExistsException;
import com.fintech.exception.ResourceNotFoundException;
import com.fintech.repository.UserRepository;
import com.fintech.service.UserService;
import com.fintech.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// All public methods are transactional by default to ensure consistent reads and automatic rollback on unchecked exception

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "User already exists with email: " +request.getEmail()
            );
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Created user '{}' with role {}", savedUser, savedUser.getRole());

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return userMapper.toResponse(
                userRepository.findByIdAndActiveTrue(id)
                        .orElseThrow(() -> ResourceNotFoundException.forUser(id))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Role role, Pageable pageable) {
        if(role != null) {
            return userRepository.findAllByRoleAndActiveTrue(role, pageable)
                    .map(userMapper::toResponse);
        }

        return userRepository.findAllByActiveTrue(pageable)
                .map(userMapper::toResponse);
    }

    @Override
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> ResourceNotFoundException.forUser(id));

        if(request.getName() != null) user.setName(request.getName());
        if(request.getRole() != null) user.setRole(request.getRole());
        if(request.getActive() != null) user.setActive(request.getActive());

        User savedUser = userRepository.save(user);
        log.info("Updated user '{}': role={}, active={}", savedUser, savedUser.getRole(), savedUser.isActive());

        return userMapper.toResponse(savedUser);
    }

    @Override
    public void deactivateUser(UUID id) {
        User user = userRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> ResourceNotFoundException.forUser(id));

        user.setActive(false);
        userRepository.save(user);
        log.info("Deactivated user '{}'", user.getEmail());
    }
}
