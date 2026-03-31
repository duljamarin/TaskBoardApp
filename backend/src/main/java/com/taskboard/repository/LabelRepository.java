package com.taskboard.repository;

import com.taskboard.model.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Label entity operations.
 */
@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {

    /**
     * Find all labels for a board ordered by name.
     */
    List<Label> findByBoardIdOrderByNameAsc(Long boardId);

    /**
     * Find a label by board and name (for uniqueness check).
     */
    Optional<Label> findByBoardIdAndName(Long boardId, String name);

    /**
     * Check if a label name already exists in a board.
     */
    boolean existsByBoardIdAndName(Long boardId, String name);

    /**
     * Find label with board eagerly loaded.
     */
    @Query("SELECT l FROM Label l JOIN FETCH l.board WHERE l.id = :id")
    Optional<Label> findByIdWithBoard(@Param("id") Long id);
}

