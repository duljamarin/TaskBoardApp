package com.taskboard.controller;

import com.taskboard.model.dto.BoardMemberDTO;
import com.taskboard.security.CurrentUser;
import com.taskboard.security.UserPrincipal;
import com.taskboard.service.BoardMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/boards/{boardId}/members")
@RequiredArgsConstructor
public class BoardMemberController {

    private final BoardMemberService boardMemberService;

    @GetMapping
    @PreAuthorize("@authorizationService.canAccessBoard(#boardId)")
    public ResponseEntity<List<BoardMemberDTO>> getMembers(
            @PathVariable Long boardId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("GET /api/v1/boards/{}/members - User: {}", boardId, currentUser.getUsername());
        return ResponseEntity.ok(boardMemberService.getMembers(boardId));
    }

    @PostMapping("/{userId}")
    @PreAuthorize("@authorizationService.canModifyBoard(#boardId)")
    public ResponseEntity<BoardMemberDTO> addMember(
            @PathVariable Long boardId,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "MEMBER") String role,
            @CurrentUser UserPrincipal currentUser) {
        log.info("POST /api/v1/boards/{}/members/{} - User: {} - Adding member with role {}",
                boardId, userId, currentUser.getUsername(), role);
        BoardMemberDTO member = boardMemberService.addMember(boardId, userId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@authorizationService.canModifyBoard(#boardId)")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long boardId,
            @PathVariable Long userId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("DELETE /api/v1/boards/{}/members/{} - User: {}", boardId, userId, currentUser.getUsername());
        boardMemberService.removeMember(boardId, userId);
        return ResponseEntity.noContent().build();
    }
}
