package com.taskboard.service;

import com.taskboard.exception.ResourceNotFoundException;
import com.taskboard.messaging.producer.EventPublisher;
import com.taskboard.model.dto.*;
import com.taskboard.model.entity.*;
import com.taskboard.model.event.BoardCreatedEvent;
import com.taskboard.repository.BoardRepository;
import com.taskboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for board operations.
 * Handles CRUD operations with caching and event publishing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;
    private final ActivityLogService activityLogService;

    /**
     * Get all non-archived boards with lists and cards.
     * Returns full board details so the UI can display list/card counts.
     */
    @Cacheable(value = "boards", key = "'all'")
    @Transactional(readOnly = true)
    public List<BoardDTO> getAllBoards() {
        log.debug("Fetching all boards with lists and cards from database");

        // Fetch all boards with lists in one query
        List<Board> boards = boardRepository.findAllByArchivedFalseWithLists();

        // If there are boards with lists, fetch all their cards in one query per board
        if (!boards.isEmpty()) {
            for (Board board : boards) {
                if (!board.getLists().isEmpty()) {
                    // This fetches cards for all lists of this board
                    boardRepository.findListsWithCardsByBoardId(board.getId());
                }
            }
        }

        // Convert to DTOs with full details
        return boards.stream()
                .map(this::convertToDTOWithDetails)
                .collect(Collectors.toList());
    }

    /**
     * Get a board by ID with all lists and cards.
     * Uses two queries to avoid MultipleBagFetchException.
     */
    @Cacheable(value = "boards", key = "#id")
    @Transactional(readOnly = true)
    public BoardDTO getBoardById(Long id) {
        log.debug("Fetching board with id: {}", id);

        // First query: fetch board with lists
        Board board = boardRepository.findByIdWithListsAndCards(id)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", id));

        // Second query: fetch all cards for the lists (this initializes the cards collection)
        if (!board.getLists().isEmpty()) {
            boardRepository.findListsWithCardsByBoardId(id);
            // Third query: fetch labels for all cards in this board
            boardRepository.findCardsWithLabelsByBoardId(id);
        }

        return convertToDTOWithDetails(board);
    }

    /**
     * Create a new board with the specified user as owner.
     */
    @CacheEvict(value = "boards", allEntries = true)
    @Transactional
    public BoardDTO createBoard(CreateBoardRequest request, Long ownerId) {
        log.info("Creating new board: {} for user: {}", request.getName(), ownerId);

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));

        Board board = Board.builder()
                .name(request.getName())
                .description(request.getDescription())
                .color(request.getColor() != null ? request.getColor() : "#3498db")
                .owner(owner)
                .archived(false)
                .build();

        board = boardRepository.save(board);
        log.info("Created board with id: {} for user: {}", board.getId(), owner.getUsername());

        // Publish event
        publishBoardCreatedEvent(board);

        // Log activity
        logBoardCreated(board);

        return convertToDTO(board);
    }

    /**
     * Update an existing board.
     */
    @CacheEvict(value = "boards", allEntries = true)
    @Transactional
    public BoardDTO updateBoard(Long id, CreateBoardRequest request) {
        log.info("Updating board with id: {}", id);

        Board board = boardRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", id));

        board.setName(request.getName());
        board.setDescription(request.getDescription());
        if (request.getColor() != null) {
            board.setColor(request.getColor());
        }

        if (request.getOwnerId() != null) {
            User owner = userRepository.findById(request.getOwnerId()).orElse(null);
            board.setOwner(owner);
        }

        board = boardRepository.save(board);
        log.info("Updated board: {}", board.getName());

        // Log activity
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("board_name", board.getName());
        activityLogService.logActivity(board, board.getOwner(), ActivityType.BOARD_UPDATED,
                String.format("Board '%s' was updated", board.getName()), metadata);

        return convertToDTO(board);
    }

    /**
     * Soft delete (archive) a board.
     */
    @CacheEvict(value = "boards", allEntries = true)
    @Transactional
    public void deleteBoard(Long id) {
        log.info("Archiving board with id: {}", id);

        Board board = boardRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", id));

        board.setArchived(true);
        boardRepository.save(board);

        log.info("Archived board: {}", board.getName());

        // Log activity
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("board_name", board.getName());
        activityLogService.logActivity(board, board.getOwner(), ActivityType.BOARD_ARCHIVED,
                String.format("Board '%s' was archived", board.getName()), metadata);
    }

    /**
     * Publish board created event to RabbitMQ.
     */
    private void publishBoardCreatedEvent(Board board) {
        BoardCreatedEvent event = BoardCreatedEvent.builder()
                .boardId(board.getId())
                .boardName(board.getName())
                .description(board.getDescription())
                .color(board.getColor())
                .createdByUserId(board.getOwner() != null ? board.getOwner().getId() : null)
                .createdByUsername(board.getOwner() != null ? board.getOwner().getUsername() : "system")
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishBoardCreated(event);
    }

    /**
     * Log board creation activity.
     */
    private void logBoardCreated(Board board) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("board_name", board.getName());
        metadata.put("color", board.getColor());

        activityLogService.logActivity(board, board.getOwner(), ActivityType.BOARD_CREATED,
                String.format("Board '%s' was created", board.getName()), metadata);
    }

    /**
     * Convert Board entity to DTO (basic info only).
     */
    private BoardDTO convertToDTO(Board board) {
        return BoardDTO.builder()
                .id(board.getId())
                .name(board.getName())
                .description(board.getDescription())
                .color(board.getColor())
                .ownerId(board.getOwner() != null ? board.getOwner().getId() : null)
                .ownerUsername(board.getOwner() != null ? board.getOwner().getUsername() : null)
                .archived(board.getArchived())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }

    /**
     * Convert Board entity to DTO with lists and cards.
     */
    private BoardDTO convertToDTOWithDetails(Board board) {
        BoardDTO dto = convertToDTO(board);

        List<ListDTO> listDTOs = board.getLists().stream()
                .map(this::convertListToDTO)
                .collect(Collectors.toList());

        dto.setLists(listDTOs);
        return dto;
    }

    /**
     * Convert BoardList entity to DTO with cards.
     */
    private ListDTO convertListToDTO(BoardList list) {
        List<CardDTO> cardDTOs = list.getCards().stream()
                .map(CardMapper::toDTO)
                .collect(Collectors.toList());

        return ListDTO.builder()
                .id(list.getId())
                .name(list.getName())
                .boardId(list.getBoard().getId())
                .position(list.getPosition())
                .cards(cardDTOs)
                .createdAt(list.getCreatedAt())
                .updatedAt(list.getUpdatedAt())
                .build();
    }
}

