package com.taskboard.service;

import com.taskboard.exception.ResourceNotFoundException;
import com.taskboard.model.dto.*;
import com.taskboard.model.entity.*;
import com.taskboard.repository.BoardMemberRepository;
import com.taskboard.repository.BoardRepository;
import com.taskboard.repository.UserRepository;
import com.taskboard.service.event.BoardCreatedAppEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final BoardMemberRepository boardMemberRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Cacheable(value = "boards", key = "'user:' + #userId")
    @Transactional(readOnly = true)
    public List<BoardDTO> getAllBoards(Long userId, boolean isAdminOrModerator) {
        log.debug("Fetching boards for user {} (admin/mod: {})", userId, isAdminOrModerator);

        List<Board> boards = isAdminOrModerator
                ? boardRepository.findAllByArchivedFalseWithLists()
                : boardRepository.findByMemberUserIdAndArchivedFalseWithLists(userId);

        // Batch-fetch cards for ALL boards in one query (fixes N+1)
        if (!boards.isEmpty()) {
            List<Long> boardIds = boards.stream().map(Board::getId).collect(Collectors.toList());
            boardRepository.findListsWithCardsByBoardIds(boardIds);
        }

        return boards.stream()
                .map(this::convertToDTOWithDetails)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "boards", key = "#id")
    @Transactional(readOnly = true)
    public BoardDTO getBoardById(Long id) {
        log.debug("Fetching board with id: {}", id);

        Board board = boardRepository.findByIdWithListsAndCards(id)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", id));

        if (!board.getLists().isEmpty()) {
            boardRepository.findListsWithCardsByBoardId(id);
            boardRepository.findCardsWithLabelsByBoardId(id);
        }

        return convertToDTOWithDetails(board);
    }

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

        boardMemberRepository.save(BoardMember.builder()
                .board(board)
                .user(owner)
                .role(BoardMemberRole.OWNER)
                .build());

        logBoardCreated(board);

        eventPublisher.publishEvent(BoardCreatedAppEvent.builder()
                .boardId(board.getId())
                .boardName(board.getName())
                .description(board.getDescription())
                .color(board.getColor())
                .createdByUserId(owner.getId())
                .createdByUsername(owner.getUsername())
                .build());

        return convertToDTO(board);
    }

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

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("board_name", board.getName());
        activityLogService.logActivity(board, board.getOwner(), ActivityType.BOARD_UPDATED,
                String.format("Board '%s' was updated", board.getName()), metadata);

        return convertToDTO(board);
    }

    @CacheEvict(value = "boards", allEntries = true)
    @Transactional
    public void deleteBoard(Long id) {
        log.info("Archiving board with id: {}", id);

        Board board = boardRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", id));

        board.setArchived(true);
        boardRepository.save(board);

        log.info("Archived board: {}", board.getName());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("board_name", board.getName());
        activityLogService.logActivity(board, board.getOwner(), ActivityType.BOARD_ARCHIVED,
                String.format("Board '%s' was archived", board.getName()), metadata);
    }

    private void logBoardCreated(Board board) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("board_name", board.getName());
        metadata.put("color", board.getColor());

        activityLogService.logActivity(board, board.getOwner(), ActivityType.BOARD_CREATED,
                String.format("Board '%s' was created", board.getName()), metadata);
    }

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

    private BoardDTO convertToDTOWithDetails(Board board) {
        BoardDTO dto = convertToDTO(board);

        List<ListDTO> listDTOs = board.getLists().stream()
                .map(this::convertListToDTO)
                .collect(Collectors.toList());

        dto.setLists(listDTOs);
        return dto;
    }

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
