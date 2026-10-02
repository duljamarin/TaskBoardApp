package com.taskboard.messaging.producer;

import com.taskboard.model.event.BoardCreatedEvent;
import com.taskboard.model.event.CardCreatedEvent;
import com.taskboard.model.event.CardMovedEvent;
import com.taskboard.model.event.CommentAddedEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Event publisher for sending messages to RabbitMQ.
 *
 * <p>Each event type is registered once against its exchange and routing key; {@link #publish}
 * then looks the destination up, so adding an event type means adding a single registry entry
 * rather than another publish method.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    /** Where an event type is sent. */
    private record Destination(String exchange, String routingKey) {}

    private final RabbitTemplate rabbitTemplate;

    private final Map<Class<?>, Destination> routes = new HashMap<>();

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

    @PostConstruct
    void registerRoutes() {
        routes.put(CardMovedEvent.class, new Destination(cardEventsExchange, cardMovedRoutingKey));
        routes.put(CardCreatedEvent.class, new Destination(cardEventsExchange, cardCreatedRoutingKey));
        routes.put(CommentAddedEvent.class, new Destination(cardEventsExchange, commentAddedRoutingKey));
        routes.put(BoardCreatedEvent.class, new Destination(boardEventsExchange, boardCreatedRoutingKey));
    }

    /**
     * Publish a domain event to its registered exchange.
     *
     * <p>Failures are logged and swallowed: messaging is a side effect and must not fail the
     * originating request.
     */
    public void publish(Object event) {
        Destination destination = routes.get(event.getClass());
        if (destination == null) {
            log.error("No RabbitMQ destination registered for event type: {}", event.getClass().getName());
            return;
        }

        try {
            log.info("Publishing {}: exchange={} routingKey={}",
                    event.getClass().getSimpleName(), destination.exchange(), destination.routingKey());
            rabbitTemplate.convertAndSend(destination.exchange(), destination.routingKey(), event);
            log.debug("Published {}: {}", event.getClass().getSimpleName(), event);
        } catch (Exception e) {
            log.error("Failed to publish {}: {}", event.getClass().getSimpleName(), e.getMessage(), e);
        }
    }
}
