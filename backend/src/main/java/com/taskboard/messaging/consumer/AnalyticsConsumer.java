package com.taskboard.messaging.consumer;

import com.taskboard.model.event.BoardCreatedEvent;
import com.taskboard.model.event.CardCreatedEvent;
import com.taskboard.model.event.CardMovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsConsumer {

    private static final String METRIC_KEY_PREFIX = "analytics:";

    private final MessageConverter messageConverter;
    private final StringRedisTemplate redisTemplate;

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
        redisTemplate.opsForValue().increment(METRIC_KEY_PREFIX + metricName);
    }

    public long getMetric(String metricName) {
        String value = redisTemplate.opsForValue().get(METRIC_KEY_PREFIX + metricName);
        return value != null ? Long.parseLong(value) : 0;
    }

    public Map<String, Long> getAllMetrics() {
        Map<String, Long> metrics = new HashMap<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(METRIC_KEY_PREFIX + "*")
                .count(100)
                .build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String value = redisTemplate.opsForValue().get(key);
                String metricName = key.substring(METRIC_KEY_PREFIX.length());
                metrics.put(metricName, value != null ? Long.parseLong(value) : 0);
            }
        } catch (Exception e) {
            log.error("Error scanning Redis analytics metrics: {}", e.getMessage());
        }
        return metrics;
    }
}
