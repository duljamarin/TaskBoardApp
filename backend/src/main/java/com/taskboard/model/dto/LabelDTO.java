package com.taskboard.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Label entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelDTO implements Serializable {

    private Long id;
    private String name;
    private String color;
    private Long boardId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

