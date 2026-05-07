package com.taskboard.model.dto;

import com.taskboard.model.entity.Card;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Utility class for mapping Card entities to CardDTO.
 * Centralises the conversion logic that was previously duplicated
 * in CardService, CardMovementService, BoardService and ListService.
 */
public final class CardMapper {

    private CardMapper() {}

    /**
     * Convert a Card entity to a CardDTO.
     *
     * @param card the card entity
     * @return CardDTO
     */
    public static CardDTO toDTO(Card card) {
        return CardDTO.builder()
                .id(card.getId())
                .title(card.getTitle())
                .description(card.getDescription())
                .boardId(card.getBoard() != null ? card.getBoard().getId() : null)
                .listId(card.getList().getId())
                .listName(card.getList().getName())
                .position(card.getPosition())
                .assignedToId(card.getAssignedTo() != null ? card.getAssignedTo().getId() : null)
                .assignedToUsername(card.getAssignedTo() != null ? card.getAssignedTo().getUsername() : null)
                .assignedToFullName(card.getAssignedTo() != null ? card.getAssignedTo().getFullName() : null)
                .priority(card.getPriority())
                .dueDate(card.getDueDate())
                .labels(card.getLabels() != null
                        ? card.getLabels().stream()
                            .map(label -> LabelDTO.builder()
                                    .id(label.getId())
                                    .name(label.getName())
                                    .color(label.getColor())
                                    .boardId(label.getBoard().getId())
                                    .createdAt(label.getCreatedAt())
                                    .updatedAt(label.getUpdatedAt())
                                    .build())
                            .collect(Collectors.toList())
                        : new ArrayList<>())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }
}

