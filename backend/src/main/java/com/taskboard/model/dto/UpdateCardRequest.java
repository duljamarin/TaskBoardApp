package com.taskboard.model.dto;

import com.taskboard.model.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Request object for updating an existing card.
 * Unlike CreateCardRequest, listId is not required (card stays in its current list).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCardRequest {

    @NotBlank(message = "Card title is required")
    @Size(min = 1, max = 255, message = "Card title must be between 1 and 255 characters")
    private String title;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    private Priority priority;

    private String dueDate;

    private Long assignedToId;

    public LocalDateTime getDueDateAsLocalDateTime() {
        if (dueDate == null || dueDate.isBlank()) {
            return null;
        }
        if (dueDate.contains("T")) {
            return LocalDateTime.parse(dueDate);
        }
        return LocalDate.parse(dueDate).atStartOfDay();
    }
}
