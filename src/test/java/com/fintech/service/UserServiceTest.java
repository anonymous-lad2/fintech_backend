package com.fintech.service;

import com.fintech.dto.request.CreateUserRequest;
import com.fintech.dto.response.UserResponse;
import com.fintech.entity.Role;
import com.fintech.entity.User;
import com.fintech.exception.ResourceAlreadyExistsException;
import com.fintech.repository.UserRepository;
import com.fintech.service.impl.UserServiceImpl;
import com.fintech.util.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl service;

    @Test
    @DisplayName("createUser() — should create user and hash password")
    void createUser_shouldHashPassword() {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("John Doe");
        request.setEmail("john@test.com");
        request.setPassword("Secret@123");
        request.setRole(Role.VIEWER);

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .name("John Doe")
                .email("john@test.com")
                .passwordHash("$2a$12$hashedpassword")
                .role(Role.VIEWER)
                .active(true)
                .build();

        UserResponse expectedResponse = UserResponse.builder()
                .id(savedUser.getId())
                .name("John Doe")
                .email("john@test.com")
                .role(Role.VIEWER)
                .active(true)
                .build();

        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode("Secret@123")).willReturn("$2a$12$hashedpassword");
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(userMapper.toResponse(savedUser)).willReturn(expectedResponse);

        UserResponse result = service.createUser(request);

        assertThat(result.getEmail()).isEqualTo("john@test.com");
        assertThat(result.getRole()).isEqualTo(Role.VIEWER);
        then(passwordEncoder).should().encode("Secret@123");
        then(userRepository).should().save(argThat(u ->
                u.getPasswordHash().equals("$2a$12$hashedpassword") &&
                !u.getPasswordHash().equals("Secret@123")  // raw password never stored
        ));
    }

    @Test
    @DisplayName("createUser() — should throw ResourceAlreadyExistsException for duplicate email")
    void createUser_shouldThrow_whenEmailTaken() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("existing@test.com");
        request.setPassword("Secret@123");
        request.setRole(Role.VIEWER);

        given(userRepository.existsByEmail("existing@test.com")).willReturn(true);

        assertThatThrownBy(() -> service.createUser(request))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("existing@test.com");

        then(userRepository).should(never()).save(any());
    }
}
