package com.taskboard.repository;
import com.taskboard.model.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
/**
 * Repository for Comment entity.
 * Uses LEFT JOIN FETCH on author to prevent N+1 when rendering author info.
 * The composite index (card_id, created_at ASC) makes every query O(log n).
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    /**
     * Fetch the full comment thread for a card with author eagerly loaded.
     * This is the primary read path - result is cached by CommentService.
     */
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.author WHERE c.card.id = :cardId ORDER BY c.createdAt ASC")
    List<Comment> findByCardIdWithAuthor(@Param("cardId") Long cardId);
    /**
     * Cheap count used to show the comment badge on cards in the board view.
     */
    long countByCardId(Long cardId);
}
