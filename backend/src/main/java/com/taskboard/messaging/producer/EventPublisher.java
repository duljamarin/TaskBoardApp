package com.taskboard.messaging.producer;

import com.taskboard.model.event.BoardCreatedEvent;
import com.taskboard.model.event.CardCreatedEvent;
import com.taskboard.model.event.CardMovedEvent;
import com.taskboard.model.event.CommentAddedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Event publisher for sending messages to RabbitMQ.
 * Publishes card and board events to appropriate exchanges.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${taskboard.rabbitmq.exchange.card-events:taskboard.card.events}")
    private String cardEventsExchange;

    @Value("${taskboard.rabbitmq.exchange.board-events:taskboard.board.events}")
    private String boardEventsExchange;

    @Value("${taskboard.rabbitmq.routing-key.card-moved:card.moved}")
    private String cardMovedRoutingKey;

    @Value("${taskboard.rabbitmq.routing-key.card-created:card.created}")
    private String cardCreatedRoutingKey;

    @Value("${taskboard.rabbitmq.routing-key.board-created:board.created}")
    private String boardCreatedRoutingKey;

    @Value("${taskboard.rabbitmq.routing-key.comment-added:comment.added}")
    private String commentAddedRoutingKey;

    /**
     * Publish a card moved event.
     */
    public void publishCardMoved(CardMovedEvent event) {
        try {
            log.debug("Publishing CardMovedEvent: cardId={}, from={} to={}",
                    event.getCardId(), event.getFromListName(), event.getToListName());

            rabbitTemplate.convertAndSend(cardEventsExchange, cardMovedRoutingKey, event);

            log.info("Successfully published CardMovedEvent for card: {}", event.getCardTitle());
        } catch (Exception e) {
            log.error("Failed to publish CardMovedEvent: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish a card created event.
     */
    public void publishCardCreated(CardCreatedEvent event) {
        try {
            log.debug("Publishing CardCreatedEvent: cardId={}, title={}",
                    event.getCardId(), event.getCardTitle());

            rabbitTemplate.convertAndSend(cardEventsExchange, cardCreatedRoutingKey, event);

            log.info("Successfully published CardCreatedEvent for card: {}", event.getCardTitle());
        } catch (Exception e) {
            log.error("Failed to publish CardCreatedEvent: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish a board created event.
     */
    public void publishBoardCreated(BoardCreatedEvent event) {
        try {
            log.debug("Publishing BoardCreatedEvent: boardId={}, name={}",
                    event.getBoardId(), event.getBoardName());

            rabbitTemplate.convertAndSend(boardEventsExchange, boardCreatedRoutingKey, event);

            log.info("Successfully published BoardCreatedEvent for board: {}", event.getBoardName());
        } catch (Exception e) {
            log.error("Failed to publish BoardCreatedEvent: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish a comment added event.
     * Routes through the card-events exchange so no new exchange is needed.
     */
    public void publishCommentAdded(CommentAddedEvent event) {
        try {
            log.debug("Publishing CommentAddedEvent: commentId={}, cardId={}",
                    event.getCommentId(), event.getCardId());

            rabbitTemplate.convertAndSend(cardEventsExchange, commentAddedRoutingKey, event);

            log.info("Successfully published CommentAddedEvent on card: {}", event.getCardTitle());
        } catch (Exception e) {
            log.error("Failed to publish CommentAddedEvent: {}", e.getMessage(), e);
        }
    }
}

