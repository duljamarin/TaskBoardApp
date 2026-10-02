package com.taskboard.model.dto;

import com.taskboard.model.entity.Label;

/**
 * Maps Label entities to LabelDTO.
 */
public final class LabelMapper {

    private LabelMapper() {}

    public static LabelDTO toDTO(Label label) {
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
