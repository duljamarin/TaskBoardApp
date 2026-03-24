package com.taskboard.service;

import com.taskboard.exception.ResourceNotFoundException;
import com.taskboard.messaging.producer.EventPublisher;
import com.taskboard.model.dto.CardDTO;
import com.taskboard.model.dto.CardMapper;
import com.taskboard.model.dto.CardMoveDTO;
import com.taskboard.model.dto.CreateCardRequest;
import com.taskboard.model.entity.*;
import com.taskboard.model.event.CardCreatedEvent;
import com.taskboard.repository.CardRepository;
import com.taskboard.repository.ListRepository;
import com.taskboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for card operations.
 * Handles CRUD operations with event publishing and real-time updates.
 * Card movement logic has been extracted to CardMovementService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final ListRepository listRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;
    private final ActivityLogService activityLogService;
    private final SimpMessagingTemplate messagingTemplate;
    private final CardMovementService cardMovementService;

    /**
     * Get all cards in a list.
     */
    @Transactional(readOnly = true)
    public List<CardDTO> getCardsByListId(Long listId) {
        log.debug("Fetching cards for list: {}", listId);
        return cardRepository.findByListIdOrderByPositionAsc(listId).stream()
                .map(CardMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a card by ID.
     */
    @Transactional(readOnly = true)
    public CardDTO getCardById(Long id) {
        log.debug("Fetching card with id: {}", id);
        Card card = cardRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id", id));
        return CardMapper.toDTO(card);
    }

    /**
     * Create a new card.
     */
    @CacheEvict(value = "boards", allEntries = true)
    @Transactional
    public CardDTO createCard(CreateCardRequest request, Long userId) {
        log.info("Creating new card: {} in list: {} by user: {}", request.getTitle(), request.getListId(), userId);

        BoardList list = listRepository.findById(request.getListId())
                .orElseThrow(() -> new ResourceNotFoundException("List", "id", request.getListId()));

        User assignedTo = null;
        if (request.getAssignedToId() != null) {
            assignedTo = userRepository.findById(request.getAssignedToId()).orElse(null);
        }

        // Get the user who is creating the card for activity logging
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Lock the list row to serialize concurrent position calculations
        listRepository.findByIdForUpdate(request.getListId())
                .orElseThrow(() -> new ResourceNotFoundException("List", "id", request.getListId()));

        // Determine position
        Integer position = request.getPosition();
        if (position == null) {
            position = cardRepository.findMaxPositionByListId(request.getListId()) + 1;
        } else {
            cardRepository.incrementPositionsFrom(request.getListId(), position);
        }

        Card card = Card.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .list(list)
                .position(position)
                .assignedTo(assignedTo)
                .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
                .dueDate(request.getDueDate())
                .build();

        card = cardRepository.save(card);
        log.info("Created card with id: {} by user: {}", card.getId(), creator.getUsername());

        // Publish event to RabbitMQ
        publishCardCreatedEvent(card);

        // Send WebSocket update
        sendWebSocketUpdate(list.getBoard().getId(), "CARD_CREATED", CardMapper.toDTO(card));

        // Log activity with creator
        logCardCreated(card, creator);

        return CardMapper.toDTO(card);
    }

    /**
     * Update a card.
     */
    @CacheEvict(value = "boards", allEntries = true)
    @Transactional
    public CardDTO updateCard(Long id, CreateCardRequest request) {
        log.info("Updating card with id: {}", id);

        Card card = cardRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id", id));

        card.setTitle(request.getTitle());
        card.setDescription(request.getDescription());

        if (request.getPriority() != null) {
            card.setPriority(request.getPriority());
        }

        card.setDueDate(request.getDueDate());

        if (request.getAssignedToId() != null) {
            User assignedTo = userRepository.findById(request.getAssignedToId()).orElse(null);
            card.setAssignedTo(assignedTo);
        }

        card = cardRepository.save(card);
        log.info("Updated card: {}", card.getTitle());

        // Send WebSocket update
        sendWebSocketUpdate(card.getBoard().getId(), "CARD_UPDATED", CardMapper.toDTO(card));

        // Log activity
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("card_title", card.getTitle());
        activityLogService.logActivity(card.getBoard(), card.getAssignedTo(), ActivityType.CARD_UPDATED,
                String.format("Card '%s' was updated", card.getTitle()), metadata);

        return CardMapper.toDTO(card);
    }

    /**
     * Move a card to a different list or position.
     * Delegates to CardMovementService which handles the complex movement logic.
     */
    public CardDTO moveCard(Long id, CardMoveDTO moveDTO, Long userId) {
        log.info("Delegating card move operation to CardMovementService");
        return cardMovementService.moveCard(id, moveDTO, userId);
    }

    /**
     * Delete a card.
     */
    @CacheEvict(value = "boards", allEntries = true)
    @Transactional
    public void deleteCard(Long id) {
        log.info("Deleting card with id: {}", id);

        Card card = cardRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id", id));

        String cardTitle = card.getTitle();
        Long listId = card.getList().getId();
        Integer deletedPosition = card.getPosition();
        Board board = card.getBoard();

        cardRepository.delete(card);

        // Reorder remaining cards
        cardRepository.decrementPositionsAfter(listId, deletedPosition);

        log.info("Deleted card: {}", cardTitle);

        // Send WebSocket update
        Map<String, Object> deleteData = new HashMap<>();
        deleteData.put("cardId", id);
        deleteData.put("listId", listId);
        sendWebSocketUpdate(board.getId(), "CARD_DELETED", deleteData);

        // Log activity
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("card_title", cardTitle);
        activityLogService.logActivity(board, null, ActivityType.CARD_DELETED,
                String.format("Card '%s' was deleted", cardTitle), metadata);
    }

    /**
     * Publish card created event.
     */
    private void publishCardCreatedEvent(Card card) {
        CardCreatedEvent event = CardCreatedEvent.builder()
                .cardId(card.getId())
                .cardTitle(card.getTitle())
                .boardId(card.getBoard().getId())
                .boardName(card.getBoard().getName())
                .listId(card.getList().getId())
                .listName(card.getList().getName())
                .priority(card.getPriority())
                .createdByUserId(card.getAssignedTo() != null ? card.getAssignedTo().getId() : null)
                .createdByUsername(card.getAssignedTo() != null ? card.getAssignedTo().getUsername() : "system")
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishCardCreated(event);
    }


    /**
     * Send WebSocket update to board subscribers.
     */
    private void sendWebSocketUpdate(Long boardId, String eventType, Object data) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", eventType);
            message.put("data", data);
            message.put("timestamp", LocalDateTime.now());

            String destination = "/topic/board/" + boardId;
            messagingTemplate.convertAndSend(destination, (Object) message);
            log.debug("Sent WebSocket update for board {}: {}", boardId, eventType);
        } catch (Exception e) {
            log.error("Failed to send WebSocket update: {}", e.getMessage());
        }
    }

    /**
     * Log card created activity.
     */
    private void logCardCreated(Card card, User creator) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("card_title", card.getTitle());
        metadata.put("list_name", card.getList().getName());
        metadata.put("priority", card.getPriority().name());
        metadata.put("created_by", creator.getUsername());

        activityLogService.logActivity(card.getBoard(), creator, ActivityType.CARD_CREATED,
                String.format("Card '%s' was created in '%s' by %s",
                    card.getTitle(), card.getList().getName(), creator.getUsername()), metadata);
    }
}
