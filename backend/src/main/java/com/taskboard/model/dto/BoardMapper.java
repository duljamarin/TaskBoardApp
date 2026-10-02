package com.taskboard.model.dto;

import com.taskboard.model.entity.Board;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps Board entities to BoardDTO.
 */
public final class BoardMapper {

    private BoardMapper() {}

    /**
     * Convert without touching the (lazily loaded) lists collection.
     */
    public static BoardDTO toDTO(Board board) {
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
     * Convert including lists and their cards. Requires those collections to be initialised.
     */
    public static BoardDTO toDTOWithDetails(Board board) {
        BoardDTO dto = toDTO(board);
        List<ListDTO> lists = board.getLists().stream()
                .map(ListMapper::toDTOWithCards)
                .collect(Collectors.toList());
        dto.setLists(lists);
        return dto;
    }
}
