package com.taskboard.service;

import com.taskboard.exception.ResourceNotFoundException;
import com.taskboard.model.dto.RoleAssignmentRequest;
import com.taskboard.model.dto.UserDTO;
import com.taskboard.model.entity.Role;
import com.taskboard.model.entity.User;
import com.taskboard.repository.RoleRepository;
import com.taskboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        log.info("Admin: Fetching all users");
        return userRepository.findAll().stream()
                .map(this::toUserDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long userId) {
        log.info("Admin: Fetching user with id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return toUserDTO(user);
    }

    @Transactional
    public UserDTO assignRole(Long userId, RoleAssignmentRequest request) {
        log.info("Admin: Assigning role {} to user {}", request.getRoleName(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role.RoleName roleName = parseRoleName(request.getRoleName());
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        if (user.getRoles().contains(role)) {
            log.info("User {} already has role {}", userId, roleName);
            return toUserDTO(user);
        }

        user.getRoles().add(role);
        userRepository.save(user);

        log.info("Role {} assigned to user {} successfully", roleName, userId);
        return toUserDTO(user);
    }

    @Transactional
    public UserDTO removeRole(Long userId, String roleName) {
        log.info("Admin: Removing role {} from user {}", roleName, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role.RoleName roleEnum = parseRoleName(roleName);
        Role role = roleRepository.findByName(roleEnum)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleEnum));

        if (roleEnum == Role.RoleName.ROLE_USER && user.getRoles().size() == 1) {
            throw new IllegalArgumentException("Cannot remove the only role from a user");
        }

        user.getRoles().remove(role);
        userRepository.save(user);

        log.info("Role {} removed from user {} successfully", roleEnum, userId);
        return toUserDTO(user);
    }

    @Transactional
    public UserDTO toggleUserStatus(Long userId) {
        log.info("Admin: Toggling active status for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setActive(!user.getActive());
        userRepository.save(user);

        log.info("User {} active status set to {}", userId, user.getActive());
        return toUserDTO(user);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(role -> Map.<String, Object>of(
                        "id", role.getId(),
                        "name", role.getName().name()
                ))
                .toList();
    }

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
