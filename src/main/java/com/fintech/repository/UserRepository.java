package com.fintech.repository;

import com.fintech.entity.Role;
import com.fintech.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndActiveTrue(String email);

    Optional<User> findByIdAndActiveTrue(UUID id);

    boolean existsByEmail(String email);

    Page<User> findAllByActiveTrue(Pageable pageable);

    Page<User> findAllByRoleAndActiveTrue(Role role, Pageable pageable);
}
