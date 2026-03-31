package com.taskboard.controller;

import com.taskboard.exception.ResourceNotFoundException;
import com.taskboard.model.dto.RoleAssignmentRequest;
import com.taskboard.model.dto.UserDTO;
import com.taskboard.model.entity.Role;
import com.taskboard.model.entity.User;
import com.taskboard.repository.RoleRepository;
import com.taskboard.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for admin operations.
 * Only accessible by users with ADMIN role.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * Get all users with their roles.
     */
    @GetMapping("/users")
    @Transactional(readOnly = true)
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        log.info("Admin: Fetching all users");
        List<UserDTO> users = userRepository.findAll().stream()
                .map(this::toUserDTO)
                .toList();
        return ResponseEntity.ok(users);
    }

    /**
     * Get a single user by ID.
     */
    @GetMapping("/users/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long userId) {
        log.info("Admin: Fetching user with id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return ResponseEntity.ok(toUserDTO(user));
    }

    /**
     * Assign a role to a user.
     */
    @PostMapping("/users/{userId}/roles")
    @Transactional
    public ResponseEntity<UserDTO> assignRole(
            @PathVariable Long userId,
            @Valid @RequestBody RoleAssignmentRequest request) {
        log.info("Admin: Assigning role {} to user {}", request.getRoleName(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role.RoleName roleName = parseRoleName(request.getRoleName());
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        if (user.getRoles().contains(role)) {
            log.info("User {} already has role {}", userId, roleName);
            return ResponseEntity.ok(toUserDTO(user));
        }

        user.getRoles().add(role);
        userRepository.save(user);

        log.info("Role {} assigned to user {} successfully", roleName, userId);
        return ResponseEntity.ok(toUserDTO(user));
    }

    /**
     * Remove a role from a user.
     */
    @DeleteMapping("/users/{userId}/roles/{roleName}")
    @Transactional
    public ResponseEntity<UserDTO> removeRole(
            @PathVariable Long userId,
            @PathVariable String roleName) {
        log.info("Admin: Removing role {} from user {}", roleName, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role.RoleName roleEnum = parseRoleName(roleName);
        Role role = roleRepository.findByName(roleEnum)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleEnum));

        // Prevent removing the last ROLE_USER from a user
        if (roleEnum == Role.RoleName.ROLE_USER && user.getRoles().size() == 1) {
            throw new IllegalArgumentException("Cannot remove the only role from a user");
        }

        user.getRoles().remove(role);
        userRepository.save(user);

        log.info("Role {} removed from user {} successfully", roleEnum, userId);
        return ResponseEntity.ok(toUserDTO(user));
    }

    /**
     * Toggle user active status (enable/disable).
     */
    @PatchMapping("/users/{userId}/status")
    @Transactional
    public ResponseEntity<UserDTO> toggleUserStatus(@PathVariable Long userId) {
        log.info("Admin: Toggling active status for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setActive(!user.getActive());
        userRepository.save(user);

        log.info("User {} active status set to {}", userId, user.getActive());
        return ResponseEntity.ok(toUserDTO(user));
    }

    /**
     * Get all available roles.
     */
    @GetMapping("/roles")
    public ResponseEntity<List<Map<String, Object>>> getAllRoles() {
        List<Map<String, Object>> roles = roleRepository.findAll().stream()
                .map(role -> Map.<String, Object>of(
                        "id", role.getId(),
                        "name", role.getName().name()
                ))
                .toList();
        return ResponseEntity.ok(roles);
    }

    // --- Helper methods ---

    private UserDTO toUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .active(user.getActive())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .toList())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private Role.RoleName parseRoleName(String name) {
        String normalized = name.toUpperCase().trim();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        try {
            return Role.RoleName.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role name: " + name +
                    ". Valid roles are: ROLE_USER, ROLE_ADMIN, ROLE_MODERATOR");
        }
    }
}

