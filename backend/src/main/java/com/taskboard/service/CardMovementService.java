package com.taskboard.service;

import com.taskboard.exception.ResourceNotFoundException;
import com.taskboard.messaging.producer.EventPublisher;
import com.taskboard.model.dto.CardDTO;
import com.taskboard.model.dto.CardMapper;
import com.taskboard.model.dto.CardMoveDTO;
import com.taskboard.model.entity.*;
import com.taskboard.model.event.CardMovedEvent;
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
import java.util.Map;

/**
 * Service for card movement operations.
 * Handles the complex logic of moving cards between lists with proper position management.
 * This service was extracted from CardService following the Single Responsibility Principle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardMovementService {

    private final CardRepository cardRepository;
    private final ListRepository listRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;
    private final ActivityLogService activityLogService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Move a card to a different list or position.
     * This is a complex operation that:
     * 1. Updates positions in the old list
     * 2. Updates positions in the new list
     * 3. Moves the card
     * 4. Publishes events
     * 5. Sends WebSocket updates
     * 6. Logs activity
     *
     * @param cardId the card to move
     * @param moveDTO the target list and position
     * @param userId the user performing the move
     * @return the updated card
     */
    @CacheEvict(value = "boards", allEntries = true)
    @Transactional
    public CardDTO moveCard(Long cardId, CardMoveDTO moveDTO, Long userId) {
        log.info("Moving card {} to list {} at position {} by user: {}",
                cardId, moveDTO.getNewListId(), moveDTO.getNewPosition(), userId);

        Card card = cardRepository.findByIdWithDetails(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id", cardId));

        BoardList newList = listRepository.findById(moveDTO.getNewListId())
                .orElseThrow(() -> new ResourceNotFoundException("List", "id", moveDTO.getNewListId()));

        User mover = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Validate the move
        validateMove(card, newList);

        // Lock the affected list(s) to serialize concurrent position changes.
        // Always acquire locks in a consistent order (by ID) to prevent deadlocks.
        long oldListId2 = card.getList().getId();
        long newListId2 = newList.getId();
        if (oldListId2 <= newListId2) {
            listRepository.findByIdForUpdate(oldListId2)
                    .orElseThrow(() -> new ResourceNotFoundException("List", "id", oldListId2));
            if (oldListId2 != newListId2) {
                listRepository.findByIdForUpdate(newListId2)
                        .orElseThrow(() -> new ResourceNotFoundException("List", "id", newListId2));
            }
        } else {
            listRepository.findByIdForUpdate(newListId2)
                    .orElseThrow(() -> new ResourceNotFoundException("List", "id", newListId2));
            listRepository.findByIdForUpdate(oldListId2)
                    .orElseThrow(() -> new ResourceNotFoundException("List", "id", oldListId2));
        }

        // Store old values for event
        Long oldListId = card.getList().getId();
        String oldListName = card.getList().getName();
        Integer oldPosition = card.getPosition();

        // Perform the move
        if (oldListId.equals(moveDTO.getNewListId())) {
            moveWithinSameList(card, moveDTO.getNewPosition());
        } else {
            moveToDifferentList(card, newList, moveDTO.getNewPosition());
        }

        card = cardRepository.save(card);
        log.info("Moved card '{}' from '{}' to '{}'", card.getTitle(), oldListName, newList.getName());

        // Publish event to RabbitMQ
        publishCardMovedEvent(card, oldListId, oldListName, oldPosition, mover);

        // Send WebSocket update
        sendWebSocketUpdate(card, oldListId, newList.getId());

        // Log activity
        logCardMoved(card, oldListName, mover);

        return CardMapper.toDTO(card);
    }

    /**
     * Validate that the card can be moved to the target list.
     */
    private void validateMove(Card card, BoardList targetList) {
        // Verify both lists belong to the same board
        if (!card.getList().getBoard().getId().equals(targetList.getBoard().getId())) {
            throw new IllegalArgumentException(
                "Cannot move card to a list on a different board");
        }
    }

    /**
     * Move card within the same list (just reorder).
     */
    private void moveWithinSameList(Card card, Integer newPosition) {
        log.debug("Reordering card {} within list {} from position {} to {}",
                card.getId(), card.getList().getId(), card.getPosition(), newPosition);

        Integer oldPosition = card.getPosition();
        Long listId = card.getList().getId();

        if (oldPosition < newPosition) {
            // Moving down: decrement positions of cards after old position
            cardRepository.decrementPositionsAfter(listId, oldPosition);
            // Then increment from target position
            cardRepository.incrementPositionsFrom(listId, newPosition);
        } else if (oldPosition > newPosition) {
            // Moving up: increment positions from new position
            cardRepository.incrementPositionsFrom(listId, newPosition);
            // Then decrement after old position
            cardRepository.decrementPositionsAfter(listId, oldPosition);
        }

        card.setPosition(newPosition);
    }

    /**
     * Move card to a different list.
     */
    private void moveToDifferentList(Card card, BoardList newList, Integer newPosition) {
        log.debug("Moving card {} from list {} to list {}",
                card.getId(), card.getList().getId(), newList.getId());

        Long oldListId = card.getList().getId();
        Integer oldPosition = card.getPosition();

        // Decrement positions in old list after the card
        cardRepository.decrementPositionsAfter(oldListId, oldPosition);

        // Increment positions in new list from the target position
        cardRepository.incrementPositionsFrom(newList.getId(), newPosition);

        // Update card
        card.setList(newList);
        card.setPosition(newPosition);
    }

    /**
     * Publish card moved event to RabbitMQ.
     */
    private void publishCardMovedEvent(Card card, Long fromListId, String fromListName,
                                      Integer fromPosition, User mover) {
        CardMovedEvent event = CardMovedEvent.builder()
                .cardId(card.getId())
                .cardTitle(card.getTitle())
                .boardId(card.getBoard().getId())
                .boardName(card.getBoard().getName())
                .fromListId(fromListId)
                .fromListName(fromListName)
                .fromPosition(fromPosition)
                .toListId(card.getList().getId())
                .toListName(card.getList().getName())
                .toPosition(card.getPosition())
                .movedByUserId(mover.getId())
                .movedByUsername(mover.getUsername())
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishCardMoved(event);
    }

    /**
     * Send WebSocket update to board subscribers.
     */
    private void sendWebSocketUpdate(Card card, Long fromListId, Long toListId) {
        try {
            Map<String, Object> moveData = new HashMap<>();
            moveData.put("card", CardMapper.toDTO(card));
            moveData.put("fromListId", fromListId);
            moveData.put("toListId", toListId);

            Map<String, Object> message = new HashMap<>();
            message.put("type", "CARD_MOVED");
            message.put("data", moveData);
            message.put("timestamp", LocalDateTime.now());

            String destination = "/topic/board/" + card.getBoard().getId();
            messagingTemplate.convertAndSend(destination, (Object) message);
            log.debug("Sent WebSocket update for card move");
        } catch (Exception e) {
            log.error("Failed to send WebSocket update: {}", e.getMessage());
        }
    }

    private void logCardMoved(Card card, String fromListName, User mover) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("card_title", card.getTitle());
        metadata.put("from_list", fromListName);
        metadata.put("to_list", card.getList().getName());
        metadata.put("moved_by", mover.getUsername());

        activityLogService.logActivity(card.getBoard(), mover, ActivityType.CARD_MOVED,
                String.format("Card '%s' was moved from '%s' to '%s' by %s",
                        card.getTitle(), fromListName, card.getList().getName(), mover.getUsername()),
                metadata);
    }
}


