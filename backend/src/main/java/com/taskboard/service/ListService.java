package com.taskboard.service;

import com.taskboard.exception.ResourceNotFoundException;
import com.taskboard.model.dto.CardMapper;
import com.taskboard.model.dto.CreateListRequest;
import com.taskboard.model.dto.ListDTO;
import com.taskboard.model.dto.CardDTO;
import com.taskboard.model.entity.*;
import com.taskboard.repository.BoardRepository;
import com.taskboard.repository.ListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for list operations.
 * Handles CRUD operations for board lists.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListService {

    private final ListRepository listRepository;
    private final BoardRepository boardRepository;
    private final ActivityLogService activityLogService;

    /**
     * Get all lists for a board.
     */
    @Transactional(readOnly = true)
    public List<ListDTO> getListsByBoardId(Long boardId) {
        log.debug("Fetching lists for board: {}", boardId);

        // Verify board exists
        if (!boardRepository.existsById(boardId)) {
            throw new ResourceNotFoundException("Board", "id", boardId);
        }

        return listRepository.findByBoardIdOrderByPositionAsc(boardId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a single list by ID.
     */
    @Transactional(readOnly = true)
    public ListDTO getListById(Long id) {
        log.debug("Fetching list with id: {}", id);
        BoardList list = listRepository.findByIdWithCards(id)
                .orElseThrow(() -> new ResourceNotFoundException("List", "id", id));
        return convertToDTOWithCards(list);
    }

    /**
     * Create a new list.
     */
    @CacheEvict(value = "boards", allEntries = true)
    @Transactional
    public ListDTO createList(CreateListRequest request) {
        log.info("Creating new list: {} for board: {}", request.getName(), request.getBoardId());

        Board board = boardRepository.findByIdAndArchivedFalse(request.getBoardId())
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", request.getBoardId()));

        // Lock the board row to serialize concurrent list position calculations
        boardRepository.findByIdForUpdate(request.getBoardId())
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", request.getBoardId()));

        // Determine position
        Integer position = request.getPosition();
        if (position == null) {
            position = listRepository.findMaxPositionByBoardId(request.getBoardId()) + 1;
        } else {
            // Shift existing lists if inserting at specific position
            listRepository.incrementPositionsFrom(request.getBoardId(), position);
        }

        BoardList list = BoardList.builder()
                .name(request.getName())
                .board(board)
                .position(position)
                .build();

        list = listRepository.save(list);
        log.info("Created list with id: {}", list.getId());

        // Log activity
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("list_name", list.getName());
        metadata.put("position", list.getPosition());
        activityLogService.logActivity(board, null, ActivityType.LIST_CREATED,
                String.format("List '%s' was created", list.getName()), metadata);

        return convertToDTO(list);
    }

    /**
     * Update a list.
     */
    @CacheEvict(value = "boards", allEntries = true)
    @Transactional
    public ListDTO updateList(Long id, CreateListRequest request) {
        log.info("Updating list with id: {}", id);

        BoardList list = listRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("List", "id", id));

        list.setName(request.getName());

        // Handle position change if specified
        if (request.getPosition() != null && !request.getPosition().equals(list.getPosition())) {
            // Reorder logic would go here
            list.setPosition(request.getPosition());
        }

        list = listRepository.save(list);
        log.info("Updated list: {}", list.getName());

        // Log activity
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("list_name", list.getName());
        activityLogService.logActivity(list.getBoard(), null, ActivityType.LIST_UPDATED,
                String.format("List '%s' was updated", list.getName()), metadata);

        return convertToDTO(list);
    }

    /**
     * Delete a list.
     */
    @CacheEvict(value = "boards", allEntries = true)
    @Transactional
    public void deleteList(Long id) {
        log.info("Deleting list with id: {}", id);

        BoardList list = listRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("List", "id", id));

        String listName = list.getName();
        Board board = list.getBoard();
        Integer deletedPosition = list.getPosition();

        listRepository.delete(list);

        // Reorder remaining lists
        listRepository.decrementPositionsAfter(board.getId(), deletedPosition);

        log.info("Deleted list: {}", listName);

        // Log activity
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("list_name", listName);
        activityLogService.logActivity(board, null, ActivityType.LIST_DELETED,
                String.format("List '%s' was deleted", listName), metadata);
    }

    /**
     * Convert BoardList entity to DTO (without cards).
     */
    private ListDTO convertToDTO(BoardList list) {
        return ListDTO.builder()
                .id(list.getId())
                .name(list.getName())
                .boardId(list.getBoard().getId())
                .position(list.getPosition())
                .createdAt(list.getCreatedAt())
                .updatedAt(list.getUpdatedAt())
                .build();
    }

    /**
     * Convert BoardList entity to DTO with cards.
     */
    private ListDTO convertToDTOWithCards(BoardList list) {
        ListDTO dto = convertToDTO(list);

        List<CardDTO> cardDTOs = list.getCards().stream()
                .map(this::convertCardToDTO)
                .collect(Collectors.toList());

        dto.setCards(cardDTOs);
        return dto;
    }

    /**
     * Count all lists in the system (used by analytics).
     */
    @Transactional(readOnly = true)
    public long countAllLists() {
        return listRepository.count();
    }

    /**
     * Convert Card entity to DTO.
     */
    private CardDTO convertCardToDTO(Card card) {
        return CardMapper.toDTO(card);
    }
}

