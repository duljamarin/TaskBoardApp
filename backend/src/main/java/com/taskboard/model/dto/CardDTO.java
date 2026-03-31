package com.taskboard.model.dto;

import com.taskboard.model.entity.Priority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Card entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDTO implements Serializable {

    private Long id;
    private String title;
    private String description;
    private Long listId;
    private String listName;
    private Integer position;
    private Long assignedToId;
    private String assignedToUsername;
    private String assignedToFullName;
    private Priority priority;
    private LocalDateTime dueDate;

    @Builder.Default
    private List<LabelDTO> labels = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

