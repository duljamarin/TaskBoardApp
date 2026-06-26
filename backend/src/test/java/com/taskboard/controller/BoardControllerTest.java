package com.taskboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskboard.model.dto.BoardDTO;
import com.taskboard.model.dto.CreateBoardRequest;
import com.taskboard.security.AuthorizationService;
import com.taskboard.security.WithMockUserPrincipal;
import com.taskboard.service.ActivityLogService;
import com.taskboard.service.BoardMemberService;
import com.taskboard.service.BoardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class BoardControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    private ObjectMapper objectMapper;

    @MockitoBean
    private BoardService boardService;

    @MockitoBean
    private ActivityLogService activityLogService;

    @MockitoBean
    private BoardMemberService boardMemberService;

    @MockitoBean
    private AuthorizationService authorizationService;

    private BoardDTO testBoard;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testBoard = BoardDTO.builder()
                .id(1L)
                .name("Test Board")
                .description("Test Description")
                .color("#3498db")
                .archived(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Mock authorization service to allow all operations in tests
        when(authorizationService.canAccessBoard(anyLong())).thenReturn(true);
        when(authorizationService.canModifyBoard(anyLong())).thenReturn(true);
        when(authorizationService.canDeleteBoard(anyLong())).thenReturn(true);
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "testuser", roles = {"USER"})
    void getAllBoards_ShouldReturnBoardsList() throws Exception {
        List<BoardDTO> boards = Arrays.asList(testBoard);
        when(authorizationService.isAdminOrModerator()).thenReturn(false);
        when(boardService.getAllBoards(1L, false)).thenReturn(boards);

        mockMvc.perform(get("/api/v1/boards"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Board"));
    }

    @Test
    @WithMockUserPrincipal(id = 2L, username = "otheruser", roles = {"USER"})
    void getAllBoards_RegularUser_ShouldOnlySeeOwnBoards() throws Exception {
        // User 2 has no boards — service returns empty for non-admin
        when(authorizationService.isAdminOrModerator()).thenReturn(false);
        when(boardService.getAllBoards(2L, false)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/boards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUserPrincipal(id = 3L, username = "adminuser", roles = {"ADMIN"})
    void getAllBoards_Admin_ShouldSeeAllBoards() throws Exception {
        List<BoardDTO> boards = Arrays.asList(testBoard);
        when(authorizationService.isAdminOrModerator()).thenReturn(true);
        when(boardService.getAllBoards(3L, true)).thenReturn(boards);

        mockMvc.perform(get("/api/v1/boards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "testuser", roles = {"USER"})
    void getBoard_ShouldReturnBoard() throws Exception {
        when(boardService.getBoardById(1L)).thenReturn(testBoard);

        mockMvc.perform(get("/api/v1/boards/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Board"));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "testuser", roles = {"USER"})
    void createBoard_ShouldReturnCreatedBoard() throws Exception {
        CreateBoardRequest request = CreateBoardRequest.builder()
                .name("New Board")
                .description("New Description")
                .color("#e74c3c")
                .build();

        when(boardService.createBoard(any(CreateBoardRequest.class), anyLong())).thenReturn(testBoard);

        mockMvc.perform(post("/api/v1/boards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "testuser", roles = {"USER"})
    void createBoard_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        CreateBoardRequest request = CreateBoardRequest.builder()
                .name("") // Invalid: empty name
                .build();

        mockMvc.perform(post("/api/v1/boards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "testuser", roles = {"USER"})
    void deleteBoard_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/boards/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}

