package com.taskboard.controller;

import com.taskboard.model.dto.ActivityLogDTO;
import com.taskboard.model.dto.BoardDTO;
import com.taskboard.model.dto.CreateBoardRequest;
import com.taskboard.security.AuthorizationService;
import com.taskboard.security.CurrentUser;
import com.taskboard.security.UserPrincipal;
import com.taskboard.service.ActivityLogService;
import com.taskboard.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for board operations.
 * All endpoints require authentication. Modification requires board ownership or admin role.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final ActivityLogService activityLogService;
    private final AuthorizationService authorizationService;

    /**
     * Get boards visible to the current user.
     * Regular users see only their own boards; admins/moderators see all.
     */
    @GetMapping
    public ResponseEntity<List<BoardDTO>> getAllBoards(@CurrentUser UserPrincipal currentUser) {
        log.info("GET /api/v1/boards - User: {} - Getting boards", currentUser.getUsername());
        List<BoardDTO> boards = boardService.getAllBoards(
                currentUser.getId(), authorizationService.isAdminOrModerator());
        return ResponseEntity.ok(boards);
    }

    /**
     * Get a board by ID with all lists and cards.
     * Requires access to the board.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.canAccessBoard(#id)")
    public ResponseEntity<BoardDTO> getBoard(@PathVariable Long id, @CurrentUser UserPrincipal currentUser) {
        log.info("GET /api/v1/boards/{} - User: {} - Getting board", id, currentUser.getUsername());
        BoardDTO board = boardService.getBoardById(id);
        return ResponseEntity.ok(board);
    }

    /**
     * Create a new board.
     * The authenticated user becomes the owner.
     */
    @PostMapping
    public ResponseEntity<BoardDTO> createBoard(
            @Valid @RequestBody CreateBoardRequest request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("POST /api/v1/boards - User: {} - Creating board: {}", currentUser.getUsername(), request.getName());
        BoardDTO board = boardService.createBoard(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(board);
    }

    /**
     * Update a board.
     * Requires board ownership or admin role.
     */
    @PutMapping("/{id}")
    @PreAuthorize("@authorizationService.canModifyBoard(#id)")
    public ResponseEntity<BoardDTO> updateBoard(
            @PathVariable Long id,
            @Valid @RequestBody CreateBoardRequest request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("PUT /api/v1/boards/{} - User: {} - Updating board", id, currentUser.getUsername());
        BoardDTO board = boardService.updateBoard(id, request);
        return ResponseEntity.ok(board);
    }

    /**
     * Delete (archive) a board.
     * Requires board ownership or admin role.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.canDeleteBoard(#id)")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long id, @CurrentUser UserPrincipal currentUser) {
        log.info("DELETE /api/v1/boards/{} - User: {} - Deleting board", id, currentUser.getUsername());
        boardService.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get recent activity for a board.
     * Requires access to the board.
     */
    @GetMapping("/{id}/activity")
    @PreAuthorize("@authorizationService.canAccessBoard(#id)")
    public ResponseEntity<List<ActivityLogDTO>> getBoardActivity(
            @PathVariable Long id,
            @RequestParam(defaultValue = "20") int limit,
            @CurrentUser UserPrincipal currentUser) {
        log.info("GET /api/v1/boards/{}/activity - User: {} - Getting activity", id, currentUser.getUsername());
        List<ActivityLogDTO> activity = activityLogService.getRecentActivity(id, limit);
        return ResponseEntity.ok(activity);
    }
}

