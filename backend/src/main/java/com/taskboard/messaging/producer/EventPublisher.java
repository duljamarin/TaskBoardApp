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

    public void publishCardMoved(CardMovedEvent event) {
        try {
            log.info("Publishing CardMovedEvent: cardId={} exchange={} routingKey={}",
                    event.getCardId(), cardEventsExchange, cardMovedRoutingKey);
            rabbitTemplate.convertAndSend(cardEventsExchange, cardMovedRoutingKey, event);
            log.debug("Published CardMovedEvent for card '{}'", event.getCardTitle());
        } catch (Exception e) {
            log.error("Failed to publish CardMovedEvent: {}", e.getMessage(), e);
        }
    }

    public void publishCardCreated(CardCreatedEvent event) {
        try {
            log.info("Publishing CardCreatedEvent: cardId={} assignedTo={} exchange={} routingKey={}",
                    event.getCardId(), event.getAssignedToUserId(), cardEventsExchange, cardCreatedRoutingKey);
            rabbitTemplate.convertAndSend(cardEventsExchange, cardCreatedRoutingKey, event);
            log.debug("Published CardCreatedEvent for card '{}'", event.getCardTitle());
        } catch (Exception e) {
            log.error("Failed to publish CardCreatedEvent: {}", e.getMessage(), e);
        }
    }

    public void publishBoardCreated(BoardCreatedEvent event) {
        try {
            log.info("Publishing BoardCreatedEvent: exchange={} routingKey={}",
                    boardEventsExchange, boardCreatedRoutingKey);
            rabbitTemplate.convertAndSend(boardEventsExchange, boardCreatedRoutingKey, event);
            log.debug("Published BoardCreatedEvent for board '{}'", event.getBoardName());
        } catch (Exception e) {
            log.error("Failed to publish BoardCreatedEvent: {}", e.getMessage(), e);
        }
    }

    public void publishCommentAdded(CommentAddedEvent event) {
        try {
            log.info("Publishing CommentAddedEvent: exchange={} routingKey={}",
                    cardEventsExchange, commentAddedRoutingKey);
            rabbitTemplate.convertAndSend(cardEventsExchange, commentAddedRoutingKey, event);
            log.debug("Published CommentAddedEvent on card '{}'", event.getCardTitle());
        } catch (Exception e) {
            log.error("Failed to publish CommentAddedEvent: {}", e.getMessage(), e);
        }
    }
}
