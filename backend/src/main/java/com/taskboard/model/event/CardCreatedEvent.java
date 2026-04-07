package com.taskboard.model.event;

import com.taskboard.model.entity.Priority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event published when a new card is created.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardCreatedEvent implements Serializable {

    private Long cardId;
    private String cardTitle;
    private Long boardId;
    private String boardName;
    private Long listId;
    private String listName;
    private Priority priority;
    private Long assignedToUserId;
    private Long createdByUserId;
    private String createdByUsername;
    private LocalDateTime dueDate;
    private LocalDateTime timestamp;
}

