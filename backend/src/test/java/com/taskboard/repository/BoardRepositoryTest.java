package com.taskboard.repository;

import com.taskboard.model.entity.Board;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BoardRepositoryTest {

    @Autowired
    private BoardRepository boardRepository;

    @Test
    void findAllByArchivedFalseWithLists_ShouldReturnOnlyNonArchivedBoards() {
        // Given
        Board activeBoard = Board.builder()
                .name("Active Board")
                .archived(false)
                .build();
        Board archivedBoard = Board.builder()
                .name("Archived Board")
                .archived(true)
                .build();

        boardRepository.save(activeBoard);
        boardRepository.save(archivedBoard);

        // When
        List<Board> result = boardRepository.findAllByArchivedFalseWithLists();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Active Board");
    }

    @Test
    void findByIdAndArchivedFalse_WithArchivedBoard_ShouldReturnEmpty() {
        // Given
        Board archivedBoard = Board.builder()
                .name("Archived Board")
                .archived(true)
                .build();
        archivedBoard = boardRepository.save(archivedBoard);

        // When
        Optional<Board> result = boardRepository.findByIdAndArchivedFalse(archivedBoard.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void save_ShouldPersistBoard() {
        // Given
        Board board = Board.builder()
                .name("Test Board")
                .description("Test Description")
                .color("#3498db")
                .archived(false)
                .build();

        // When
        Board savedBoard = boardRepository.save(board);

        // Then
        assertThat(savedBoard.getId()).isNotNull();
        assertThat(savedBoard.getName()).isEqualTo("Test Board");
    }
}

