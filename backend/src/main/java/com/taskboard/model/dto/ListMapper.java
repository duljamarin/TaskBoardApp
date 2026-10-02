package com.taskboard.model.dto;

import com.taskboard.model.entity.BoardList;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps BoardList entities to ListDTO.
 * Centralises conversion previously duplicated in ListService and BoardService.
 */
public final class ListMapper {

    private ListMapper() {}

    /**
     * Convert without touching the (lazily loaded) cards collection.
     */
    public static ListDTO toDTO(BoardList list) {
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
     * Convert including the list's cards. Requires the cards collection to be initialised.
     */
    public static ListDTO toDTOWithCards(BoardList list) {
        ListDTO dto = toDTO(list);
        List<CardDTO> cards = list.getCards().stream()
                .map(CardMapper::toDTO)
                .collect(Collectors.toList());
        dto.setCards(cards);
        return dto;
    }
}
