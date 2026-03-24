package com.taskboard.model.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.io.Serializable;
import java.time.LocalDateTime;
/**
 * Entity representing a comment on a card.
 * Author is nullable so comments are preserved when a user account is deleted.
 */
@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comments_card_created", columnList = "card_id, created_at ASC"),
        @Index(name = "idx_comments_author",  columnList = "author_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Builder.Default
    private Boolean edited = false;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
