package com.taskboard.service;

import com.taskboard.exception.ResourceNotFoundException;
import com.taskboard.messaging.producer.EventPublisher;
import com.taskboard.model.dto.BoardDTO;
import com.taskboard.model.dto.CreateBoardRequest;
import com.taskboard.model.entity.Board;
import com.taskboard.model.entity.User;
import com.taskboard.repository.BoardRepository;
import com.taskboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private BoardService boardService;

    private Board testBoard;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .active(true)
                .build();

        testBoard = Board.builder()
                .id(1L)
                .name("Test Board")
                .description("Test Description")
                .color("#3498db")
                .owner(testUser)
                .archived(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllBoards_ShouldReturnAllNonArchivedBoards() {
        List<Board> boards = Arrays.asList(testBoard);
        when(boardRepository.findAllByArchivedFalseWithLists()).thenReturn(boards);

        List<BoardDTO> result = boardService.getAllBoards();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Board");
        verify(boardRepository).findAllByArchivedFalseWithLists();
    }

    @Test
    void getBoardById_WithValidId_ShouldReturnBoard() {
        when(boardRepository.findByIdWithListsAndCards(1L)).thenReturn(Optional.of(testBoard));

        BoardDTO result = boardService.getBoardById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Board");
    }

    @Test
    void getBoardById_WithInvalidId_ShouldThrowException() {
        when(boardRepository.findByIdWithListsAndCards(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.getBoardById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Board");
    }

    @Test
    void createBoard_ShouldCreateAndReturnBoard() {
        CreateBoardRequest request = CreateBoardRequest.builder()
                .name("New Board")
                .description("New Description")
                .color("#e74c3c")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);

        BoardDTO result = boardService.createBoard(request, 1L);

        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        verify(boardRepository).save(any(Board.class));
        verify(eventPublisher).publishBoardCreated(any());
    }

    @Test
    void deleteBoard_ShouldArchiveBoard() {
        when(boardRepository.findByIdAndArchivedFalse(1L)).thenReturn(Optional.of(testBoard));
        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);

        boardService.deleteBoard(1L);

        verify(boardRepository).save(argThat(board -> board.getArchived()));
    }
}

