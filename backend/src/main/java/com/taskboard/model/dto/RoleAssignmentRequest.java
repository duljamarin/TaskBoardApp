package com.taskboard.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for assigning/removing a role to/from a user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentRequest {

    @NotBlank(message = "Role name is required")
    private String roleName;
}

