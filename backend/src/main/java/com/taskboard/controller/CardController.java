package com.taskboard.controller;

import com.taskboard.model.dto.CardDTO;
import com.taskboard.model.dto.CardMoveDTO;
import com.taskboard.model.dto.CreateCardRequest;
import com.taskboard.security.CurrentUser;
import com.taskboard.security.UserPrincipal;
import com.taskboard.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for card operations.
 * All endpoints require authentication and appropriate board access.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    /**
     * Get all cards in a list.
     * Requires access to the list's board.
     */
    @GetMapping("/list/{listId}")
    @PreAuthorize("@authorizationService.canAccessList(#listId)")
    public ResponseEntity<List<CardDTO>> getCardsByList(
            @PathVariable Long listId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("GET /api/v1/cards/list/{} - User: {} - Getting cards", listId, currentUser.getUsername());
        List<CardDTO> cards = cardService.getCardsByListId(listId);
        return ResponseEntity.ok(cards);
    }

    /**
     * Get a card by ID.
     * Requires access to the card's board.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.canAccessCard(#id)")
    public ResponseEntity<CardDTO> getCard(@PathVariable Long id, @CurrentUser UserPrincipal currentUser) {
        log.info("GET /api/v1/cards/{} - User: {} - Getting card", id, currentUser.getUsername());
        CardDTO card = cardService.getCardById(id);
        return ResponseEntity.ok(card);
    }

    /**
     * Create a new card.
     * Requires modification access to the list's board.
     */
    @PostMapping
    @PreAuthorize("@authorizationService.canModifyList(#request.listId)")
    public ResponseEntity<CardDTO> createCard(
            @Valid @RequestBody CreateCardRequest request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("POST /api/v1/cards - User: {} - Creating card: {} in list: {}",
                currentUser.getUsername(), request.getTitle(), request.getListId());
        CardDTO card = cardService.createCard(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }

    /**
     * Update a card.
     * Requires modification access to the card's board.
     */
    @PutMapping("/{id}")
    @PreAuthorize("@authorizationService.canModifyCard(#id)")
    public ResponseEntity<CardDTO> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody CreateCardRequest request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("PUT /api/v1/cards/{} - User: {} - Updating card", id, currentUser.getUsername());
        CardDTO card = cardService.updateCard(id, request);
        return ResponseEntity.ok(card);
    }

    /**
     * Move a card to a different list or position.
     * Requires modification access to the card's board.
     */
    @PostMapping("/{id}/move")
    @PreAuthorize("@authorizationService.canModifyCard(#id)")
    public ResponseEntity<CardDTO> moveCard(
            @PathVariable Long id,
            @Valid @RequestBody CardMoveDTO moveDTO,
            @CurrentUser UserPrincipal currentUser) {
        log.info("POST /api/v1/cards/{}/move - User: {} - Moving card to list: {} position: {}",
                id, currentUser.getUsername(), moveDTO.getNewListId(), moveDTO.getNewPosition());
        CardDTO card = cardService.moveCard(id, moveDTO, currentUser.getId());
        return ResponseEntity.ok(card);
    }

    /**
     * Delete a card.
     * Requires modification access to the card's board.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.canModifyCard(#id)")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id, @CurrentUser UserPrincipal currentUser) {
        log.info("DELETE /api/v1/cards/{} - User: {} - Deleting card", id, currentUser.getUsername());
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}

