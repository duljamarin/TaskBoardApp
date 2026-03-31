package com.taskboard.service;

import com.taskboard.exception.ResourceNotFoundException;
import com.taskboard.model.dto.CreateLabelRequest;
import com.taskboard.model.dto.LabelDTO;
import com.taskboard.model.entity.Board;
import com.taskboard.model.entity.Card;
import com.taskboard.model.entity.Label;
import com.taskboard.repository.BoardRepository;
import com.taskboard.repository.CardRepository;
import com.taskboard.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for label operations.
 * Handles CRUD for board-scoped labels and assignment/removal on cards.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelRepository labelRepository;
    private final BoardRepository boardRepository;
    private final CardRepository cardRepository;

    /**
     * Get all labels for a board.
     */
    @Transactional(readOnly = true)
    public List<LabelDTO> getLabelsByBoardId(Long boardId) {
        log.debug("Fetching labels for board: {}", boardId);
        if (!boardRepository.existsById(boardId)) {
            throw new ResourceNotFoundException("Board", "id", boardId);
        }
        return labelRepository.findByBoardIdOrderByNameAsc(boardId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new label for a board.
     */
    @CacheEvict(value = "boards", allEntries = true)
    @Transactional
    public LabelDTO createLabel(Long boardId, CreateLabelRequest request) {
        log.info("Creating label '{}' for board: {}", request.getName(), boardId);

        Board board = boardRepository.findByIdAndArchivedFalse(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", boardId));

        if (labelRepository.existsByBoardIdAndName(boardId, request.getName())) {
            throw new IllegalArgumentException("A label with name '" + request.getName() + "' already exists in this board");
        }

        Label label = Label.builder()
                .name(request.getName())
                .color(request.getColor() != null ? request.getColor() : "#3498db")
                .board(board)
                .build();

        label = labelRepository.save(label);
        log.info("Created label with id: {}", label.getId());
        return toDTO(label);
    }

    /**
     * Update an existing label.
     */
    @CacheEvict(value = "boards", allEntries = true)
    @Transactional
    public LabelDTO updateLabel(Long labelId, CreateLabelRequest request) {
        log.info("Updating label with id: {}", labelId);

        Label label = labelRepository.findByIdWithBoard(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label", "id", labelId));

        // Check uniqueness if name changed
        if (!label.getName().equals(request.getName())
                && labelRepository.existsByBoardIdAndName(label.getBoard().getId(), request.getName())) {
            throw new IllegalArgumentException("A label with name '" + request.getName() + "' already exists in this board");
        }

        label.setName(request.getName());
        if (request.getColor() != null) {
            label.setColor(request.getColor());
        }

        label = labelRepository.save(label);
        log.info("Updated label: {}", label.getName());
        return toDTO(label);
    }

    /**
     * Delete a label.
     */
    @CacheEvict(value = "boards", allEntries = true)
    @Transactional
    public void deleteLabel(Long labelId) {
        log.info("Deleting label with id: {}", labelId);

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label", "id", labelId));

        labelRepository.delete(label);
        log.info("Deleted label: {}", label.getName());
    }

    /**
     * Assign a label to a card.
     */
    @CacheEvict(value = "boards", allEntries = true)
    @Transactional
    public void assignLabelToCard(Long cardId, Long labelId) {
        log.info("Assigning label {} to card {}", labelId, cardId);

        Card card = cardRepository.findByIdWithDetails(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id", cardId));

        Label label = labelRepository.findByIdWithBoard(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label", "id", labelId));

        // Verify label and card belong to the same board
        if (!card.getBoard().getId().equals(label.getBoard().getId())) {
            throw new IllegalArgumentException("Label and card must belong to the same board");
        }

        card.getLabels().add(label);
        cardRepository.save(card);
        log.info("Assigned label '{}' to card '{}'", label.getName(), card.getTitle());
    }

    /**
     * Remove a label from a card.
     */
    @CacheEvict(value = "boards", allEntries = true)
    @Transactional
    public void removeLabelFromCard(Long cardId, Long labelId) {
        log.info("Removing label {} from card {}", labelId, cardId);

        Card card = cardRepository.findByIdWithDetails(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id", cardId));

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label", "id", labelId));

        card.getLabels().remove(label);
        cardRepository.save(card);
        log.info("Removed label '{}' from card '{}'", label.getName(), card.getTitle());
    }

    /**
     * Get labels for a specific card.
     */
    @Transactional(readOnly = true)
    public List<LabelDTO> getLabelsForCard(Long cardId) {
        Card card = cardRepository.findByIdWithDetails(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id", cardId));

        return card.getLabels().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert Label entity to DTO.
     */
    private LabelDTO toDTO(Label label) {
        return LabelDTO.builder()
                .id(label.getId())
                .name(label.getName())
                .color(label.getColor())
                .boardId(label.getBoard().getId())
                .createdAt(label.getCreatedAt())
                .updatedAt(label.getUpdatedAt())
                .build();
    }
}

