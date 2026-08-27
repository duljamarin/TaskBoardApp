package com.taskboard.service.event;

import com.taskboard.model.dto.CardDTO;
import com.taskboard.model.entity.Priority;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Published after a card is created (within the transaction).
 * Consumed after commit to send WebSocket + RabbitMQ notifications.
 */
@Getter
@Builder
public class CardCreatedAppEvent {
    private final Long cardId;
    private final String cardTitle;
    private final Long boardId;
    private final String boardName;
    private final Long listId;
    private final String listName;
    private final Priority priority;
    private final LocalDateTime dueDate;
    private final Long assignedToUserId;
    private final Long createdByUserId;
    private final String createdByUsername;
    private final CardDTO cardDTO;
}
