package com.taskboard.controller;

import com.taskboard.model.dto.RoleAssignmentRequest;
import com.taskboard.model.dto.UserDTO;
import com.taskboard.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getUserById(userId));
    }

    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<UserDTO> assignRole(
            @PathVariable Long userId,
            @Valid @RequestBody RoleAssignmentRequest request) {
        return ResponseEntity.ok(adminService.assignRole(userId, request));
    }

    @DeleteMapping("/users/{userId}/roles/{roleName}")
    public ResponseEntity<UserDTO> removeRole(
            @PathVariable Long userId,
            @PathVariable String roleName) {
        return ResponseEntity.ok(adminService.removeRole(userId, roleName));
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<UserDTO> toggleUserStatus(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.toggleUserStatus(userId));
    }

    @GetMapping("/roles")
    public ResponseEntity<List<Map<String, Object>>> getAllRoles() {
        return ResponseEntity.ok(adminService.getAllRoles());
    }
}
