package com.taskboard.repository;

import com.taskboard.model.entity.BoardList;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for BoardList entity operations.
 */
@Repository
public interface ListRepository extends JpaRepository<BoardList, Long> {

    /**
     * Find all lists for a board ordered by position.
     */
    List<BoardList> findByBoardIdOrderByPositionAsc(Long boardId);

    /**
     * Find a list with its cards.
     */
    @Query("SELECT l FROM BoardList l LEFT JOIN FETCH l.cards WHERE l.id = :id")
    Optional<BoardList> findByIdWithCards(@Param("id") Long id);

    /**
     * Lock a list row for update to serialize concurrent position changes.
     * Use this before any operation that reads and then modifies card positions
     * within this list, to prevent lost-update race conditions under READ COMMITTED.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM BoardList l WHERE l.id = :id")
    Optional<BoardList> findByIdForUpdate(@Param("id") Long id);

    /**
     * Get the maximum position in a board.
     */
    @Query("SELECT COALESCE(MAX(l.position), -1) FROM BoardList l WHERE l.board.id = :boardId")
    Integer findMaxPositionByBoardId(@Param("boardId") Long boardId);

    /**
     * Update positions of lists after a certain position.
     */
    @Modifying
    @Query("UPDATE BoardList l SET l.position = l.position + 1 " +
           "WHERE l.board.id = :boardId AND l.position >= :position")
    void incrementPositionsFrom(@Param("boardId") Long boardId, @Param("position") Integer position);

    /**
     * Update positions of lists after deletion.
     */
    @Modifying
    @Query("UPDATE BoardList l SET l.position = l.position - 1 " +
           "WHERE l.board.id = :boardId AND l.position > :position")
    void decrementPositionsAfter(@Param("boardId") Long boardId, @Param("position") Integer position);

    /**
     * Count lists in a board.
     */
    long countByBoardId(Long boardId);
}

