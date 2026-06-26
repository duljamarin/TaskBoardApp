package com.taskboard.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Board entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardDTO implements Serializable {

    private Long id;
    private String name;
    private String description;
    private String color;
    private Long ownerId;
    private String ownerUsername;
    private Boolean archived;

    @Builder.Default
    private List<ListDTO> lists = new ArrayList<>();

    @Builder.Default
    private List<BoardMemberDTO> members = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

