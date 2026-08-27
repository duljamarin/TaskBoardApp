package com.taskboard.service;

import com.taskboard.exception.ResourceNotFoundException;
import com.taskboard.model.dto.CardDTO;
import com.taskboard.model.dto.CardMapper;
import com.taskboard.model.dto.CardMoveDTO;
import com.taskboard.model.dto.CreateCardRequest;
import com.taskboard.model.dto.UpdateCardRequest;
import com.taskboard.model.entity.*;
import com.taskboard.repository.BoardMemberRepository;
import com.taskboard.repository.CardRepository;
import com.taskboard.repository.ListRepository;
import com.taskboard.repository.UserRepository;
import com.taskboard.service.event.CardCreatedAppEvent;
import com.taskboard.service.event.CardDeletedAppEvent;
import com.taskboard.service.event.CardUpdatedAppEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for card operations.
 * Publishes Spring application events for post-commit side effects (WebSocket, RabbitMQ).
 * Card movement logic is in CardMovementService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final ListRepository listRepository;
    private final UserRepository userRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final ActivityLogService activityLogService;
    private final CardMovementService cardMovementService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<CardDTO> getCardsByListId(Long listId) {
        log.debug("Fetching cards for list: {}", listId);
        return cardRepository.findByListIdOrderByPositionAsc(listId).stream()
                .map(CardMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CardDTO getCardById(Long id) {
        log.debug("Fetching card with id: {}", id);
        Card card = cardRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id", id));
        return CardMapper.toDTO(card);
    }

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

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Lock the list row to serialize concurrent position calculations
        listRepository.findByIdForUpdate(request.getListId())
                .orElseThrow(() -> new ResourceNotFoundException("List", "id", request.getListId()));

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
                .dueDate(request.getDueDateAsLocalDateTime())
                .build();

        card = cardRepository.save(card);
        log.info("Created card with id: {} by user: {}", card.getId(), creator.getUsername());

        if (card.getAssignedTo() != null) {
            ensureBoardMembership(card.getBoard(), card.getAssignedTo());
        }

        logCardCreated(card, creator);

        CardDTO cardDTO = CardMapper.toDTO(card);
        eventPublisher.publishEvent(CardCreatedAppEvent.builder()
                .cardId(card.getId())
                .cardTitle(card.getTitle())
                .boardId(card.getBoard().getId())
                .boardName(card.getBoard().getName())
                .listId(card.getList().getId())
                .listName(card.getList().getName())
                .priority(card.getPriority())
                .dueDate(card.getDueDate())
                .assignedToUserId(card.getAssignedTo() != null ? card.getAssignedTo().getId() : null)
                .createdByUserId(userId)
                .createdByUsername(creator.getUsername())
                .cardDTO(cardDTO)
                .build());

        return cardDTO;
    }

    @CacheEvict(value = "boards", allEntries = true)
    @Transactional
    public CardDTO updateCard(Long id, UpdateCardRequest request) {
        log.info("Updating card with id: {}", id);

        Card card = cardRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id", id));

        card.setTitle(request.getTitle());
        card.setDescription(request.getDescription());

        if (request.getPriority() != null) {
            card.setPriority(request.getPriority());
        }

        card.setDueDate(request.getDueDateAsLocalDateTime());

        if (request.getAssignedToId() != null) {
            User assignedTo = userRepository.findById(request.getAssignedToId()).orElse(null);
            card.setAssignedTo(assignedTo);
        }

        card = cardRepository.save(card);
        log.info("Updated card: {}", card.getTitle());

        if (card.getAssignedTo() != null) {
            ensureBoardMembership(card.getBoard(), card.getAssignedTo());
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("card_title", card.getTitle());
        activityLogService.logActivity(card.getBoard(), card.getAssignedTo(), ActivityType.CARD_UPDATED,
                String.format("Card '%s' was updated", card.getTitle()), metadata);

        CardDTO cardDTO = CardMapper.toDTO(card);
        eventPublisher.publishEvent(CardUpdatedAppEvent.builder()
                .boardId(card.getBoard().getId())
                .cardDTO(cardDTO)
                .build());

        return cardDTO;
    }

    public CardDTO moveCard(Long id, CardMoveDTO moveDTO, Long userId) {
        log.info("Delegating card move operation to CardMovementService");
        return cardMovementService.moveCard(id, moveDTO, userId);
    }

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
        Long boardId = board.getId();

        cardRepository.delete(card);
        cardRepository.decrementPositionsAfter(listId, deletedPosition);

        log.info("Deleted card: {}", cardTitle);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("card_title", cardTitle);
        activityLogService.logActivity(board, null, ActivityType.CARD_DELETED,
                String.format("Card '%s' was deleted", cardTitle), metadata);

        eventPublisher.publishEvent(CardDeletedAppEvent.builder()
                .boardId(boardId)
                .cardId(id)
                .listId(listId)
                .build());
    }

    private void ensureBoardMembership(Board board, User user) {
        if (!boardMemberRepository.existsByBoardIdAndUserId(board.getId(), user.getId())) {
            boardMemberRepository.save(BoardMember.builder()
                    .board(board)
                    .user(user)
                    .role(BoardMemberRole.MEMBER)
                    .build());
            log.debug("Auto-added user {} as member of board {}", user.getUsername(), board.getId());
        }
    }

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
