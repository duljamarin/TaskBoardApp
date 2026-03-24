package com.taskboard.messaging.consumer;

import com.taskboard.model.event.BoardCreatedEvent;
import com.taskboard.model.event.CardCreatedEvent;
import com.taskboard.model.event.CardMovedEvent;
import com.taskboard.model.event.CommentAddedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for notification events from RabbitMQ.
 * Handles card and board events to send notifications.
 * In a production environment, this would integrate with email, SMS, or push notification services.
 */
@Slf4j
@Component
public class NotificationConsumer {

    /**
     * Handle card moved events for notifications.
     */
    @RabbitListener(queues = "${taskboard.rabbitmq.queue.notifications:taskboard.notifications}")
    public void handleCardMovedEvent(CardMovedEvent event) {
        log.info("=== NOTIFICATION: Card Moved ===");
        log.info("Card '{}' was moved from '{}' to '{}' in board '{}'",
                event.getCardTitle(),
                event.getFromListName(),
                event.getToListName(),
                event.getBoardName());
        log.info("Moved by: {}", event.getMovedByUsername());
        log.info("Timestamp: {}", event.getTimestamp());
    }

    @RabbitListener(queues = "${taskboard.rabbitmq.queue.notifications:taskboard.notifications}")
    public void handleCardCreatedEvent(CardCreatedEvent event) {
        log.info("=== NOTIFICATION: Card Created ===");
        log.info("New card '{}' created in list '{}' on board '{}'",
                event.getCardTitle(),
                event.getListName(),
                event.getBoardName());
        log.info("Priority: {}", event.getPriority());
        log.info("Created by: {}", event.getCreatedByUsername());
        log.info("Timestamp: {}", event.getTimestamp());
    }

    @RabbitListener(queues = "${taskboard.rabbitmq.queue.notifications:taskboard.notifications}")
    public void handleBoardCreatedEvent(BoardCreatedEvent event) {
        log.info("=== NOTIFICATION: Board Created ===");
        log.info("New board '{}' created", event.getBoardName());
        log.info("Description: {}", event.getDescription());
        log.info("Created by: {}", event.getCreatedByUsername());
        log.info("Timestamp: {}", event.getTimestamp());
    }

    /**
     * Handle comment added events.
     * In production: notify the card assignee and board members via email/push.
     */
    @RabbitListener(queues = "${taskboard.rabbitmq.queue.notifications:taskboard.notifications}")
    public void handleCommentAddedEvent(CommentAddedEvent event) {
        log.info("=== NOTIFICATION: Comment Added ===");
        log.info("'{}' commented on card '{}' in board '{}'",
                event.getAuthorUsername(), event.getCardTitle(), event.getBoardName());
        log.info("Preview: {}", event.getContentPreview());
        log.info("Timestamp: {}", event.getTimestamp());
    }
}

