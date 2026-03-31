package com.taskboard.controller;

import com.taskboard.model.dto.CreateLabelRequest;
import com.taskboard.model.dto.LabelDTO;
import com.taskboard.security.CurrentUser;
import com.taskboard.security.UserPrincipal;
import com.taskboard.service.LabelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for label operations.
 * Labels are scoped per board. Assignment/removal is at card level.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LabelController {

    private final LabelService labelService;

    // ── Board-level label CRUD ──────────────────────────────────────────

    /**
     * Get all labels for a board.
     */
    @GetMapping("/boards/{boardId}/labels")
    @PreAuthorize("@authorizationService.canAccessBoard(#boardId)")
    public ResponseEntity<List<LabelDTO>> getLabelsByBoard(
            @PathVariable Long boardId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("GET /api/v1/boards/{}/labels - User: {}", boardId, currentUser.getUsername());
        List<LabelDTO> labels = labelService.getLabelsByBoardId(boardId);
        return ResponseEntity.ok(labels);
    }

    /**
     * Create a new label for a board.
     */
    @PostMapping("/boards/{boardId}/labels")
    @PreAuthorize("@authorizationService.canModifyBoard(#boardId)")
    public ResponseEntity<LabelDTO> createLabel(
            @PathVariable Long boardId,
            @Valid @RequestBody CreateLabelRequest request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("POST /api/v1/boards/{}/labels - User: {} - Creating label: {}",
                boardId, currentUser.getUsername(), request.getName());
        LabelDTO label = labelService.createLabel(boardId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(label);
    }

    /**
     * Update an existing label.
     */
    @PutMapping("/labels/{id}")
    @PreAuthorize("@authorizationService.canModifyLabel(#id)")
    public ResponseEntity<LabelDTO> updateLabel(
            @PathVariable Long id,
            @Valid @RequestBody CreateLabelRequest request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("PUT /api/v1/labels/{} - User: {} - Updating label", id, currentUser.getUsername());
        LabelDTO label = labelService.updateLabel(id, request);
        return ResponseEntity.ok(label);
    }

    /**
     * Delete a label.
     */
    @DeleteMapping("/labels/{id}")
    @PreAuthorize("@authorizationService.canModifyLabel(#id)")
    public ResponseEntity<Void> deleteLabel(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        log.info("DELETE /api/v1/labels/{} - User: {} - Deleting label", id, currentUser.getUsername());
        labelService.deleteLabel(id);
        return ResponseEntity.noContent().build();
    }

    // ── Card-level label assignment ─────────────────────────────────────

    /**
     * Assign a label to a card.
     */
    @PostMapping("/cards/{cardId}/labels/{labelId}")
    @PreAuthorize("@authorizationService.canModifyCard(#cardId)")
    public ResponseEntity<Void> assignLabel(
            @PathVariable Long cardId,
            @PathVariable Long labelId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("POST /api/v1/cards/{}/labels/{} - User: {} - Assigning label",
                cardId, labelId, currentUser.getUsername());
        labelService.assignLabelToCard(cardId, labelId);
        return ResponseEntity.ok().build();
    }

    /**
     * Remove a label from a card.
     */
    @DeleteMapping("/cards/{cardId}/labels/{labelId}")
    @PreAuthorize("@authorizationService.canModifyCard(#cardId)")
    public ResponseEntity<Void> removeLabel(
            @PathVariable Long cardId,
            @PathVariable Long labelId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("DELETE /api/v1/cards/{}/labels/{} - User: {} - Removing label",
                cardId, labelId, currentUser.getUsername());
        labelService.removeLabelFromCard(cardId, labelId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get labels for a specific card.
     */
    @GetMapping("/cards/{cardId}/labels")
    @PreAuthorize("@authorizationService.canAccessCard(#cardId)")
    public ResponseEntity<List<LabelDTO>> getCardLabels(
            @PathVariable Long cardId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("GET /api/v1/cards/{}/labels - User: {}", cardId, currentUser.getUsername());
        List<LabelDTO> labels = labelService.getLabelsForCard(cardId);
        return ResponseEntity.ok(labels);
    }
}

