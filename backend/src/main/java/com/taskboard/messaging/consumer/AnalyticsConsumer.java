package com.taskboard.messaging.consumer;

import com.taskboard.model.event.BoardCreatedEvent;
import com.taskboard.model.event.CardCreatedEvent;
import com.taskboard.model.event.CardMovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsConsumer {

    private final MessageConverter messageConverter;
    private final ConcurrentHashMap<String, AtomicLong> metrics = new ConcurrentHashMap<>();

    @RabbitListener(queues = "${taskboard.rabbitmq.queue.analytics:taskboard.analytics}")
    public void handleEvent(Message message) {
        Object event = messageConverter.fromMessage(message);
        log.debug("Analytics consumer received: {}", event.getClass().getSimpleName());

        if (event instanceof CardMovedEvent e) {
            handleCardMoved(e);
        } else if (event instanceof CardCreatedEvent e) {
            handleCardCreated(e);
        } else if (event instanceof BoardCreatedEvent e) {
            handleBoardCreated(e);
        } else {
            log.warn("Unrecognized event type in analytics queue: {}", event.getClass().getName());
        }
    }

    private void handleCardMoved(CardMovedEvent event) {
        incrementMetric("cards_moved_total");
        incrementMetric("cards_moved_board_" + event.getBoardId());
        if (event.getMovedByUserId() != null) {
            incrementMetric("cards_moved_by_user_" + event.getMovedByUserId());
        }
        log.info("Analytics: Card move {} -> {} (total: {})",
                event.getFromListName(), event.getToListName(), getMetric("cards_moved_total"));
    }

    private void handleCardCreated(CardCreatedEvent event) {
        incrementMetric("cards_created_total");
        incrementMetric("cards_created_board_" + event.getBoardId());
        incrementMetric("cards_created_priority_" + event.getPriority().name().toLowerCase());
        if (event.getCreatedByUserId() != null) {
            incrementMetric("cards_created_by_user_" + event.getCreatedByUserId());
        }
        log.info("Analytics: Card created '{}' (total: {})",
                event.getCardTitle(), getMetric("cards_created_total"));
    }

    private void handleBoardCreated(BoardCreatedEvent event) {
        incrementMetric("boards_created_total");
        if (event.getCreatedByUserId() != null) {
            incrementMetric("boards_created_by_user_" + event.getCreatedByUserId());
        }
        log.info("Analytics: Board created '{}' (total: {})",
                event.getBoardName(), getMetric("boards_created_total"));
    }

    private void incrementMetric(String metricName) {
        metrics.computeIfAbsent(metricName, k -> new AtomicLong(0)).incrementAndGet();
    }

    public long getMetric(String metricName) {
        AtomicLong metric = metrics.get(metricName);
        return metric != null ? metric.get() : 0;
    }

    public ConcurrentHashMap<String, AtomicLong> getAllMetrics() {
        return new ConcurrentHashMap<>(metrics);
    }
}
