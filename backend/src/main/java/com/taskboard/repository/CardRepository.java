package com.taskboard.repository;

import com.taskboard.model.entity.Card;
import com.taskboard.model.entity.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Card entity operations.
 */
@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * Find all cards in a list ordered by position.
     */
    List<Card> findByListIdOrderByPositionAsc(Long listId);

    /**
     * Find card with list and board info.
     */
    @Query("SELECT c FROM Card c " +
           "LEFT JOIN FETCH c.list l " +
           "LEFT JOIN FETCH l.board " +
           "LEFT JOIN FETCH c.assignedTo " +
           "WHERE c.id = :id")
    Optional<Card> findByIdWithDetails(@Param("id") Long id);

    /**
     * Get the maximum position in a list.
     */
    @Query("SELECT COALESCE(MAX(c.position), -1) FROM Card c WHERE c.list.id = :listId")
    Integer findMaxPositionByListId(@Param("listId") Long listId);

    /**
     * Update positions of cards after a certain position in a list.
     */
    @Modifying
    @Query("UPDATE Card c SET c.position = c.position + 1 " +
           "WHERE c.list.id = :listId AND c.position >= :position")
    void incrementPositionsFrom(@Param("listId") Long listId, @Param("position") Integer position);

    /**
     * Update positions of cards after deletion.
     */
    @Modifying
    @Query("UPDATE Card c SET c.position = c.position - 1 " +
           "WHERE c.list.id = :listId AND c.position > :position")
    void decrementPositionsAfter(@Param("listId") Long listId, @Param("position") Integer position);

}

