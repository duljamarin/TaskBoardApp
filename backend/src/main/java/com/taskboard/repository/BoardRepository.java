package com.taskboard.repository;

import com.taskboard.model.entity.Board;
import com.taskboard.model.entity.BoardList;
import com.taskboard.model.entity.Card;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Board entity operations.
 */
@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    /**
     * Find all non-archived boards with lists eagerly loaded.
     */
    @Query("SELECT DISTINCT b FROM Board b " +
           "LEFT JOIN FETCH b.lists " +
           "WHERE b.archived = false " +
           "ORDER BY b.id DESC")
    List<Board> findAllByArchivedFalseWithLists();

    /**
     * Find a specific non-archived board by ID.
     */
    Optional<Board> findByIdAndArchivedFalse(Long id);
     /** Find board with all lists eagerly loaded (first step).
     * We cannot fetch multiple bags in one query, so we split it into two queries.
     */
    @Query("SELECT DISTINCT b FROM Board b " +
           "LEFT JOIN FETCH b.lists " +
           "WHERE b.id = :id AND b.archived = false")
    Optional<Board> findByIdWithListsAndCards(Long id);

    /**
     * Fetch cards for all lists of a specific board (second step).
     */
    @Query("SELECT DISTINCT l FROM BoardList l " +
           "LEFT JOIN FETCH l.cards " +
           "WHERE l.board.id = :boardId " +
           "ORDER BY l.position ASC")
    List<BoardList> findListsWithCardsByBoardId(Long boardId);

    /**
     * Fetch cards with their labels for a specific board (third step).
     * Prevents N+1 queries when accessing card.labels during DTO conversion.
     */
    @Query("SELECT DISTINCT c FROM Card c " +
           "LEFT JOIN FETCH c.labels " +
           "WHERE c.list.board.id = :boardId")
    List<Card> findCardsWithLabelsByBoardId(Long boardId);

    /**
     * Lock a board row for update to serialize concurrent list position changes.
     * Use this before any operation that reads and then modifies list positions
     * within this board, to prevent lost-update race conditions under READ COMMITTED.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Board b WHERE b.id = :id")
    Optional<Board> findByIdForUpdate(Long id);
}

