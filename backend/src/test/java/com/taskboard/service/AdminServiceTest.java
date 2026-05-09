package com.taskboard.service;

import com.taskboard.exception.ResourceNotFoundException;
import com.taskboard.model.dto.RoleAssignmentRequest;
import com.taskboard.model.dto.UserDTO;
import com.taskboard.model.entity.Role;
import com.taskboard.model.entity.User;
import com.taskboard.repository.RoleRepository;
import com.taskboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private AdminService adminService;

    private User testUser;
    private Role roleUser;
    private Role roleAdmin;

    @BeforeEach
    void setUp() {
        roleUser = Role.builder()
                .id(1L)
                .name(Role.RoleName.ROLE_USER)
                .build();

        roleAdmin = Role.builder()
                .id(2L)
                .name(Role.RoleName.ROLE_ADMIN)
                .build();

        Set<Role> roles = new HashSet<>();
        roles.add(roleUser);

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .active(true)
                .roles(roles)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- getAllUsers ---

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        List<UserDTO> result = adminService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("testuser");
        assertThat(result.get(0).getRoles()).containsExactly("ROLE_USER");
    }

    @Test
    void getAllUsers_WhenNoUsers_ShouldReturnEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserDTO> result = adminService.getAllUsers();

        assertThat(result).isEmpty();
    }

    // --- getUserById ---

    @Test
    void getUserById_WithValidId_ShouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserDTO result = adminService.getUserById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void getUserById_WithInvalidId_ShouldThrowResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    // --- assignRole ---

    @Test
    void assignRole_WithNewRole_ShouldAddRoleAndReturnUpdatedUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(Role.RoleName.ROLE_ADMIN)).thenReturn(Optional.of(roleAdmin));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDTO result = adminService.assignRole(1L, new RoleAssignmentRequest("ROLE_ADMIN"));

        assertThat(testUser.getRoles()).contains(roleAdmin);
        verify(userRepository).save(testUser);
    }

    @Test
    void assignRole_WhenUserAlreadyHasRole_ShouldNotSave() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(Role.RoleName.ROLE_USER)).thenReturn(Optional.of(roleUser));

        adminService.assignRole(1L, new RoleAssignmentRequest("ROLE_USER"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void assignRole_NormalizesRoleNameWithoutPrefix() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(Role.RoleName.ROLE_ADMIN)).thenReturn(Optional.of(roleAdmin));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        adminService.assignRole(1L, new RoleAssignmentRequest("admin"));

        assertThat(testUser.getRoles()).contains(roleAdmin);
    }

    @Test
    void assignRole_WithUnknownRoleName_ShouldThrowIllegalArgumentException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> adminService.assignRole(1L, new RoleAssignmentRequest("ROLE_UNKNOWN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid role name");
    }

    @Test
    void assignRole_WithInvalidUserId_ShouldThrowResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.assignRole(99L, new RoleAssignmentRequest("ROLE_ADMIN")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- removeRole ---

    @Test
    void removeRole_ShouldRemoveRoleAndReturnUpdatedUser() {
        testUser.getRoles().add(roleAdmin);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(Role.RoleName.ROLE_ADMIN)).thenReturn(Optional.of(roleAdmin));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        adminService.removeRole(1L, "ROLE_ADMIN");

        assertThat(testUser.getRoles()).doesNotContain(roleAdmin);
        verify(userRepository).save(testUser);
    }

    @Test
    void removeRole_LastRoleUser_ShouldThrowIllegalArgumentException() {
        // testUser has only ROLE_USER
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(Role.RoleName.ROLE_USER)).thenReturn(Optional.of(roleUser));

        assertThatThrownBy(() -> adminService.removeRole(1L, "ROLE_USER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot remove the only role");

        verify(userRepository, never()).save(any());
    }

    @Test
    void removeRole_WithInvalidUserId_ShouldThrowResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.removeRole(99L, "ROLE_ADMIN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- toggleUserStatus ---

    @Test
    void toggleUserStatus_WhenActive_ShouldDeactivateUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDTO result = adminService.toggleUserStatus(1L);

        assertThat(testUser.getActive()).isFalse();
        verify(userRepository).save(testUser);
    }

    @Test
    void toggleUserStatus_WhenInactive_ShouldActivateUser() {
        testUser.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        adminService.toggleUserStatus(1L);

        assertThat(testUser.getActive()).isTrue();
    }

    @Test
    void toggleUserStatus_WithInvalidId_ShouldThrowResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.toggleUserStatus(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- getAllRoles ---

    @Test
    void getAllRoles_ShouldReturnAllRoles() {
        when(roleRepository.findAll()).thenReturn(List.of(roleUser, roleAdmin));

        List<Map<String, Object>> result = adminService.getAllRoles();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(m -> m.get("name"))
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }
}
