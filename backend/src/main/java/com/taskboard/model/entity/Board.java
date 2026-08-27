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
 * Entity representing a task board.
 * A board contains multiple lists, each of which contains cards (tasks).
 */
@Entity
@Table(name = "boards")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Board implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    @Builder.Default
    private String color = "#3498db";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<BoardList> lists = new ArrayList<>();

    @Builder.Default
    private Boolean archived = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Adds a list to this board and sets the bidirectional relationship.
     */
    public void addList(BoardList list) {
        lists.add(list);
        list.setBoard(this);
    }

    /**
     * Removes a list from this board.
     */
    public void removeList(BoardList list) {
        lists.remove(list);
        list.setBoard(null);
    }
}

