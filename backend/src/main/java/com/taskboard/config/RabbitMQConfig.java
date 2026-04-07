package com.taskboard.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for event-driven messaging.
 * Defines exchanges, queues, and bindings for card and board events.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${taskboard.rabbitmq.exchange.card-events:taskboard.card.events}")
    private String cardEventsExchange;

    @Value("${taskboard.rabbitmq.exchange.board-events:taskboard.board.events}")
    private String boardEventsExchange;

    @Value("${taskboard.rabbitmq.queue.notifications:taskboard.notifications}")
    private String notificationsQueue;

    @Value("${taskboard.rabbitmq.queue.analytics:taskboard.analytics}")
    private String analyticsQueue;

    public static final String DEAD_LETTER_EXCHANGE = "taskboard.dlx";
    public static final String DEAD_LETTER_QUEUE = "taskboard.dlq";

    // Exchanges
    @Bean
    public DirectExchange cardEventsExchange() {
        return new DirectExchange(cardEventsExchange, true, false);
    }

    @Bean
    public DirectExchange boardEventsExchange() {
        return new DirectExchange(boardEventsExchange, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    // Queues
    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(notificationsQueue)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder.durable(analyticsQueue)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    // Bindings for card events
    @Bean
    public Binding notificationsCardMovedBinding(Queue notificationsQueue, DirectExchange cardEventsExchange) {
        return BindingBuilder.bind(notificationsQueue)
                .to(cardEventsExchange)
                .with("card.moved");
    }

    @Bean
    public Binding notificationsCardCreatedBinding(Queue notificationsQueue, DirectExchange cardEventsExchange) {
        return BindingBuilder.bind(notificationsQueue)
                .to(cardEventsExchange)
                .with("card.created");
    }

    @Bean
    public Binding analyticsCardMovedBinding(Queue analyticsQueue, DirectExchange cardEventsExchange) {
        return BindingBuilder.bind(analyticsQueue)
                .to(cardEventsExchange)
                .with("card.moved");
    }

    @Bean
    public Binding analyticsCardCreatedBinding(Queue analyticsQueue, DirectExchange cardEventsExchange) {
        return BindingBuilder.bind(analyticsQueue)
                .to(cardEventsExchange)
                .with("card.created");
    }

    // Bindings for board events
    @Bean
    public Binding notificationsBoardCreatedBinding(Queue notificationsQueue, DirectExchange boardEventsExchange) {
        return BindingBuilder.bind(notificationsQueue)
                .to(boardEventsExchange)
                .with("board.created");
    }

    @Bean
    public Binding analyticsBoardCreatedBinding(Queue analyticsQueue, DirectExchange boardEventsExchange) {
        return BindingBuilder.bind(analyticsQueue)
                .to(boardEventsExchange)
                .with("board.created");
    }

    // Bindings for comment events
    // Comments route to notifications only — they are not an analytics workflow metric.
    @Bean
    public Binding notificationsCommentAddedBinding(Queue notificationsQueue, DirectExchange cardEventsExchange) {
        return BindingBuilder.bind(notificationsQueue)
                .to(cardEventsExchange)
                .with("comment.added");
    }

    // Dead letter binding
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with("dlq");
    }

    /**
     * Configure JSON message converter with Jackson 3.
     * Uses JacksonJsonMessageConverter (Spring AMQP 4.0+).
     * Configures trusted packages to allow deserialization of event classes.
     */
    @Bean
    public MessageConverter messageConverter() {
        JacksonJsonMessageConverter converter =
                new JacksonJsonMessageConverter("com.taskboard.model.event", "com.taskboard.*", "java.util", "java.time");
        // Ensure __TypeId__ header drives deserialization (critical for Object-typed listener methods)
        converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.TYPE_ID);
        return converter;
    }
}

