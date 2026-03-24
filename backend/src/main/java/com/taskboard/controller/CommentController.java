package com.taskboard.controller;

import com.taskboard.model.dto.CommentDTO;
import com.taskboard.model.dto.CreateCommentRequest;
import com.taskboard.security.AuthorizationService;
import com.taskboard.security.CurrentUser;
import com.taskboard.security.UserPrincipal;
import com.taskboard.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for card comments.
 *
 * Endpoints
 * ─────────
 * GET    /api/v1/cards/{cardId}/comments   → fetch thread (Redis-cached)
 * POST   /api/v1/cards/{cardId}/comments   → add comment
 * PUT    /api/v1/comments/{id}             → edit own comment
 * DELETE /api/v1/comments/{id}             → delete (author or admin)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CommentController {

    private final CommentService commentService;
    private final AuthorizationService authorizationService;

    /**
     * Get all comments for a card.
     * Requires read access to the card's board.
     */
    @GetMapping("/api/v1/cards/{cardId}/comments")
    @PreAuthorize("@authorizationService.canAccessCard(#cardId)")
    public ResponseEntity<List<CommentDTO>> getComments(
            @PathVariable Long cardId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("GET comments for card {} by user {}", cardId, currentUser.getUsername());
        return ResponseEntity.ok(commentService.getCommentsByCardId(cardId));
    }

    /**
     * Add a comment to a card.
     * Any board member (reader) can comment — not just the owner.
     */
    @PostMapping("/api/v1/cards/{cardId}/comments")
    @PreAuthorize("@authorizationService.canAccessCard(#cardId)")
    public ResponseEntity<CommentDTO> addComment(
            @PathVariable Long cardId,
            @Valid @RequestBody CreateCommentRequest request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("POST comment on card {} by user {}", cardId, currentUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(cardId, request, currentUser.getId()));
    }

    /**
     * Edit a comment.
     * Only the original author or an admin may edit.
     */
    @PutMapping("/api/v1/comments/{id}")
    @PreAuthorize("@authorizationService.canModifyComment(#id)")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("PUT comment {} by user {}", id, currentUser.getUsername());
        return ResponseEntity.ok(
                commentService.updateComment(id, request, currentUser.getId()));
    }

    /**
     * Delete a comment.
     * Only the original author or an admin may delete.
     */
    @DeleteMapping("/api/v1/comments/{id}")
    @PreAuthorize("@authorizationService.canModifyComment(#id)")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        log.info("DELETE comment {} by user {}", id, currentUser.getUsername());
        commentService.deleteComment(id, currentUser.getId(), authorizationService.isAdmin());
        return ResponseEntity.noContent().build();
    }
}

