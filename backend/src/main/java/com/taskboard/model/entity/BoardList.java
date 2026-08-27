package com.taskboard.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a list within a board.
 * A list contains multiple cards and has a position within the board.
 */
@Entity
@Table(name = "board_lists", indexes = {
        @Index(name = "idx_board_lists_board_position", columnList = "board_id, position")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardList implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(nullable = false)
    @Builder.Default
    private Integer position = 0;

    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<Card> cards = new ArrayList<>();

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Adds a card to this list and sets the bidirectional relationship.
     */
    public void addCard(Card card) {
        cards.add(card);
        card.setList(this);
    }

    /**
     * Removes a card from this list.
     */
    public void removeCard(Card card) {
        cards.remove(card);
        card.setList(null);
    }
}

