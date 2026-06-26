package com.taskboard.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardMemberDTO implements Serializable {

    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private String role;
    private LocalDateTime createdAt;
}
