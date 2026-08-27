package com.taskboard.service.event;

import com.taskboard.messaging.producer.EventPublisher;
import com.taskboard.model.event.BoardCreatedEvent;
import com.taskboard.model.event.CardCreatedEvent;
import com.taskboard.model.event.CardMovedEvent;
import com.taskboard.model.event.CommentAddedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized handler for post-commit side effects: WebSocket broadcasts and RabbitMQ publishing.
 * Replaces the scattered TransactionHooks.afterCommit() calls and direct SimpMessagingTemplate
 * injections across services.
 *
 * All methods use @TransactionalEventListener which defaults to AFTER_COMMIT phase.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BoardEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final EventPublisher eventPublisher;

    @TransactionalEventListener
    public void onCardCreated(CardCreatedAppEvent event) {
        // RabbitMQ
        eventPublisher.publishCardCreated(CardCreatedEvent.builder()
                .cardId(event.getCardId())
                .cardTitle(event.getCardTitle())
                .boardId(event.getBoardId())
                .boardName(event.getBoardName())
                .listId(event.getListId())
                .listName(event.getListName())
                .priority(event.getPriority())
                .dueDate(event.getDueDate())
                .assignedToUserId(event.getAssignedToUserId())
                .createdByUserId(event.getCreatedByUserId())
                .createdByUsername(event.getCreatedByUsername())
                .timestamp(LocalDateTime.now())
                .build());

        // WebSocket
        sendBoardUpdate(event.getBoardId(), "CARD_CREATED", event.getCardDTO());
    }

    @TransactionalEventListener
    public void onCardUpdated(CardUpdatedAppEvent event) {
        sendBoardUpdate(event.getBoardId(), "CARD_UPDATED", event.getCardDTO());
    }

    @TransactionalEventListener
    public void onCardDeleted(CardDeletedAppEvent event) {
        Map<String, Object> deleteData = new HashMap<>();
        deleteData.put("cardId", event.getCardId());
        deleteData.put("listId", event.getListId());
        sendBoardUpdate(event.getBoardId(), "CARD_DELETED", deleteData);
    }

    @TransactionalEventListener
    public void onCardMoved(CardMovedAppEvent event) {
        // RabbitMQ
        eventPublisher.publishCardMoved(CardMovedEvent.builder()
                .cardId(event.getCardId())
                .cardTitle(event.getCardTitle())
                .boardId(event.getBoardId())
                .boardName(event.getBoardName())
                .fromListId(event.getFromListId())
                .fromListName(event.getFromListName())
                .fromPosition(event.getFromPosition())
                .toListId(event.getToListId())
                .toListName(event.getToListName())
                .toPosition(event.getToPosition())
                .movedByUserId(event.getMovedByUserId())
                .movedByUsername(event.getMovedByUsername())
                .timestamp(LocalDateTime.now())
                .build());

        // WebSocket
        Map<String, Object> moveData = new HashMap<>();
        moveData.put("card", event.getCardDTO());
        moveData.put("fromListId", event.getFromListId());
        moveData.put("toListId", event.getToListId());
        sendBoardUpdate(event.getBoardId(), "CARD_MOVED", moveData);
    }

    @TransactionalEventListener
    public void onBoardCreated(BoardCreatedAppEvent event) {
        eventPublisher.publishBoardCreated(BoardCreatedEvent.builder()
                .boardId(event.getBoardId())
                .boardName(event.getBoardName())
                .description(event.getDescription())
                .color(event.getColor())
                .createdByUserId(event.getCreatedByUserId())
                .createdByUsername(event.getCreatedByUsername())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @TransactionalEventListener
    public void onCommentAdded(CommentAddedAppEvent event) {
        // RabbitMQ
        eventPublisher.publishCommentAdded(CommentAddedEvent.builder()
                .commentId(event.getCommentId())
                .cardId(event.getCardId())
                .cardTitle(event.getCardTitle())
                .boardId(event.getBoardId())
                .boardName(event.getBoardName())
                .authorId(event.getAuthorId())
                .authorUsername(event.getAuthorUsername())
                .contentPreview(event.getContentPreview())
                .timestamp(LocalDateTime.now())
                .build());

        // WebSocket — broadcast to both board and card topics
        Map<String, Object> message = buildMessage("COMMENT_ADDED", event.getCommentDTO());
        message.put("cardId", event.getCardId());
        sendToTopic("/topic/board/" + event.getBoardId(), message);
        sendToTopic("/topic/card/" + event.getCardId(), message);
    }

    private void sendBoardUpdate(Long boardId, String eventType, Object data) {
        sendToTopic("/topic/board/" + boardId, buildMessage(eventType, data));
    }

    private Map<String, Object> buildMessage(String eventType, Object data) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", eventType);
        message.put("data", data);
        message.put("timestamp", LocalDateTime.now());
        return message;
    }

    private void sendToTopic(String destination, Object message) {
        try {
            messagingTemplate.convertAndSend(destination, message);
            log.debug("Sent WebSocket update to {}", destination);
        } catch (Exception e) {
            log.error("Failed to send WebSocket update to {}: {}", destination, e.getMessage());
        }
    }
}
