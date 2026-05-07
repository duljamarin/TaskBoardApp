package com.taskboard.repository;

import com.taskboard.model.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ActivityLog entity operations.
 */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    /**
     * Find activity logs for a board ordered by creation time.
     */
    Page<ActivityLog> findByBoardIdOrderByCreatedAtDesc(Long boardId, Pageable pageable);

    /**
     * Find recent activity logs for a board.
     */
    @Query("SELECT a FROM ActivityLog a " +
           "LEFT JOIN FETCH a.user " +
           "LEFT JOIN FETCH a.board " +
           "WHERE a.board.id = :boardId " +
           "ORDER BY a.createdAt DESC")
    List<ActivityLog> findRecentByBoardId(@Param("boardId") Long boardId, Pageable pageable);


    /**
     * Count activity logs for a board.
     */
    long countByBoardId(Long boardId);
}

