package com.fintech.dto.request;

import com.fintech.entity.Role;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    private Role role;

    private Boolean active;
}
