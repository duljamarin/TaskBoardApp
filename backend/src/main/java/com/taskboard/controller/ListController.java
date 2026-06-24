package com.taskboard.controller;

import com.taskboard.model.dto.CreateListRequest;
import com.taskboard.model.dto.ListDTO;
import com.taskboard.security.CurrentUser;
import com.taskboard.security.UserPrincipal;
import com.taskboard.service.ListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for list operations.
 * All endpoints require authentication and appropriate board access.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/lists")
@RequiredArgsConstructor
public class ListController {

    private final ListService listService;

    /**
     * Get all lists for a board.
     * Requires access to the board.
     */
    @GetMapping("/board/{boardId}")
    @PreAuthorize("@authorizationService.canAccessBoard(#boardId)")
    public ResponseEntity<List<ListDTO>> getListsByBoard(
            @PathVariable Long boardId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("GET /api/v1/lists/board/{} - User: {} - Getting lists", boardId, currentUser.getUsername());
        List<ListDTO> lists = listService.getListsByBoardId(boardId);
        return ResponseEntity.ok(lists);
    }

    /**
     * Get a list by ID.
     * Requires access to the list's board.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.canAccessList(#id)")
    public ResponseEntity<ListDTO> getList(@PathVariable Long id, @CurrentUser UserPrincipal currentUser) {
        log.info("GET /api/v1/lists/{} - User: {} - Getting list", id, currentUser.getUsername());
        ListDTO list = listService.getListById(id);
        return ResponseEntity.ok(list);
    }

    /**
     * Create a new list.
     * Requires modification access to the board.
     */
    @PostMapping
    @PreAuthorize("@authorizationService.canModifyBoard(#request.boardId)")
    public ResponseEntity<ListDTO> createList(
            @Valid @RequestBody CreateListRequest request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("POST /api/v1/lists - User: {} - Creating list: {} for board: {}",
                currentUser.getUsername(), request.getName(), request.getBoardId());
        ListDTO list = listService.createList(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(list);
    }

    /**
     * Update a list.
     * Requires modification access to the list's board.
     */
    @PutMapping("/{id}")
    @PreAuthorize("@authorizationService.canModifyList(#id)")
    public ResponseEntity<ListDTO> updateList(
            @PathVariable Long id,
            @Valid @RequestBody CreateListRequest request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("PUT /api/v1/lists/{} - User: {} - Updating list", id, currentUser.getUsername());
        ListDTO list = listService.updateList(id, request);
        return ResponseEntity.ok(list);
    }

    /**
     * Delete a list.
     * Requires modification access to the list's board.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.canModifyList(#id)")
    public ResponseEntity<Void> deleteList(@PathVariable Long id, @CurrentUser UserPrincipal currentUser) {
        log.info("DELETE /api/v1/lists/{} - User: {} - Deleting list", id, currentUser.getUsername());
        listService.deleteList(id);
        return ResponseEntity.noContent().build();
    }
}

