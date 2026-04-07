package com.taskboard.messaging.consumer;

import com.taskboard.model.entity.NotificationType;
import com.taskboard.model.event.BoardCreatedEvent;
import com.taskboard.model.event.CardCreatedEvent;
import com.taskboard.model.event.CardMovedEvent;
import com.taskboard.model.event.CommentAddedEvent;
import com.taskboard.repository.CardRepository;
import com.taskboard.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final CardRepository cardRepository;
    private final MessageConverter messageConverter;

    @RabbitListener(queues = "${taskboard.rabbitmq.queue.notifications:taskboard.notifications}")
    public void handleEvent(Message message) {
        Object event = messageConverter.fromMessage(message);
        log.info("Notification consumer received: {} ({})", event.getClass().getSimpleName(), event);

        if (event instanceof CardMovedEvent e) {
            handleCardMoved(e);
        } else if (event instanceof CardCreatedEvent e) {
            handleCardCreated(e);
        } else if (event instanceof CommentAddedEvent e) {
            handleCommentAdded(e);
        } else if (event instanceof BoardCreatedEvent e) {
            log.info("Board '{}' created by '{}'", e.getBoardName(), e.getCreatedByUsername());
        } else {
            log.warn("Unrecognized event type: {}", event.getClass().getName());
        }
    }

    private void handleCardMoved(CardMovedEvent event) {
        log.info("Card '{}' moved from '{}' to '{}'",
                event.getCardTitle(), event.getFromListName(), event.getToListName());

        cardRepository.findByIdWithDetails(event.getCardId()).ifPresent(card -> {
            if (card.getAssignedTo() != null
                    && !card.getAssignedTo().getId().equals(event.getMovedByUserId())) {
                notificationService.createNotification(
                        card.getAssignedTo().getId(),
                        NotificationType.CARD_MOVED,
                        "Card moved",
                        String.format("'%s' was moved from '%s' to '%s'",
                                event.getCardTitle(), event.getFromListName(), event.getToListName()),
                        event.getCardId(), "CARD");
            }
        });
    }

    private void handleCardCreated(CardCreatedEvent event) {
        log.info("Card '{}' created, assignedTo={}, createdBy={}",
                event.getCardTitle(), event.getAssignedToUserId(), event.getCreatedByUserId());

        if (event.getAssignedToUserId() != null
                && !event.getAssignedToUserId().equals(event.getCreatedByUserId())) {
            notificationService.createNotification(
                    event.getAssignedToUserId(),
                    NotificationType.CARD_ASSIGNED,
                    "Card assigned to you",
                    String.format("You were assigned to '%s' in board '%s'",
                            event.getCardTitle(), event.getBoardName()),
                    event.getCardId(), "CARD");
        }
    }

    private void handleCommentAdded(CommentAddedEvent event) {
        log.info("'{}' commented on card '{}'", event.getAuthorUsername(), event.getCardTitle());

        cardRepository.findByIdWithDetails(event.getCardId()).ifPresent(card -> {
            if (card.getAssignedTo() != null
                    && !card.getAssignedTo().getId().equals(event.getAuthorId())) {
                notificationService.createNotification(
                        card.getAssignedTo().getId(),
                        NotificationType.COMMENT_ADDED,
                        "New comment",
                        String.format("'%s' commented on '%s'",
                                event.getAuthorUsername(), event.getCardTitle()),
                        event.getCommentId(), "COMMENT");
            }
        });
    }
}
