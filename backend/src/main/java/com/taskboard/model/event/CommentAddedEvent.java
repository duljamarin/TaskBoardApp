package com.taskboard.model.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;
/**
 * Event published to RabbitMQ when a comment is added to a card.
 * Consumed by NotificationConsumer to notify relevant users.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentAddedEvent implements Serializable {
    private Long   commentId;
    private Long   cardId;
    private String cardTitle;
    private Long   boardId;
    private String boardName;
    private Long   authorId;
    private String authorUsername;
    /** First 100 characters of the comment for notification previews. */
    private String contentPreview;
    private LocalDateTime timestamp;
}
