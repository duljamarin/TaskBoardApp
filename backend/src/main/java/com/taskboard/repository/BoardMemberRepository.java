package com.taskboard.repository;

import com.taskboard.model.entity.BoardMember;
import com.taskboard.model.entity.BoardMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardMemberRepository extends JpaRepository<BoardMember, Long> {

    boolean existsByBoardIdAndUserId(Long boardId, Long userId);

    Optional<BoardMember> findByBoardIdAndUserId(Long boardId, Long userId);

    List<BoardMember> findByBoardId(Long boardId);

    @Query("SELECT bm.user.id FROM BoardMember bm WHERE bm.board.id = :boardId")
    List<Long> findUserIdsByBoardId(@Param("boardId") Long boardId);

    @Query("SELECT bm.role FROM BoardMember bm WHERE bm.board.id = :boardId AND bm.user.id = :userId")
    Optional<BoardMemberRole> findRoleByBoardIdAndUserId(@Param("boardId") Long boardId, @Param("userId") Long userId);

    void deleteByBoardIdAndUserId(Long boardId, Long userId);
}
