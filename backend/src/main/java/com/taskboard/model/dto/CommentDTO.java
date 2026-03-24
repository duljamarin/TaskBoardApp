package com.taskboard.model.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;
/**
 * DTO for a card comment. Serializable for Redis caching.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO implements Serializable {
    private Long id;
    private Long cardId;
    private Long authorId;
    private String authorUsername;
    private String authorFullName;
    private String content;
    private Boolean edited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
