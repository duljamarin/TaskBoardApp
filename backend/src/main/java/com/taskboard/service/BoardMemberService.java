package com.taskboard.service;

import com.taskboard.exception.ResourceNotFoundException;
import com.taskboard.model.dto.BoardMemberDTO;
import com.taskboard.model.entity.Board;
import com.taskboard.model.entity.BoardMember;
import com.taskboard.model.entity.BoardMemberRole;
import com.taskboard.model.entity.User;
import com.taskboard.repository.BoardMemberRepository;
import com.taskboard.repository.BoardRepository;
import com.taskboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardMemberService {

    private final BoardMemberRepository boardMemberRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<BoardMemberDTO> getMembers(Long boardId) {
        return boardMemberRepository.findByBoardId(boardId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public BoardMemberDTO addMember(Long boardId, Long userId, String role) {
        Board board = boardRepository.findByIdAndArchivedFalse(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", boardId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        BoardMemberRole memberRole;
        try {
            memberRole = BoardMemberRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            memberRole = BoardMemberRole.MEMBER;
        }

        // Upsert: update role if already a member
        final BoardMemberRole finalRole = memberRole;
        BoardMember member = boardMemberRepository.findByBoardIdAndUserId(boardId, userId)
                .map(existing -> {
                    existing.setRole(finalRole);
                    return existing;
                })
                .orElse(BoardMember.builder()
                        .board(board)
                        .user(user)
                        .role(memberRole)
                        .build());

        member = boardMemberRepository.save(member);
        log.info("Added/updated member {} on board {} with role {}", user.getUsername(), boardId, memberRole);
        return toDTO(member);
    }

    @Transactional
    public void removeMember(Long boardId, Long userId) {
        BoardMember member = boardMemberRepository.findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("BoardMember", "userId", userId));

        if (member.getRole() == BoardMemberRole.OWNER) {
            throw new IllegalStateException("Cannot remove the board owner");
        }

        boardMemberRepository.delete(member);
        log.info("Removed member {} from board {}", userId, boardId);
    }

    private BoardMemberDTO toDTO(BoardMember member) {
        return BoardMemberDTO.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .username(member.getUser().getUsername())
                .fullName(member.getUser().getFullName())
                .role(member.getRole().name())
                .createdAt(member.getCreatedAt())
                .build();
    }
}
