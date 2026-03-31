package com.taskboard.security;

import com.taskboard.exception.ResourceNotFoundException;
import com.taskboard.model.entity.Board;
import com.taskboard.model.entity.BoardList;
import com.taskboard.model.entity.Card;
import com.taskboard.model.entity.Comment;
import com.taskboard.model.entity.Label;
import com.taskboard.repository.BoardRepository;
import com.taskboard.repository.CardRepository;
import com.taskboard.repository.CommentRepository;
import com.taskboard.repository.LabelRepository;
import com.taskboard.repository.ListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for authorization checks.
 * Provides methods to verify user permissions on resources.
 */
@Slf4j
@Service("authorizationService")
@RequiredArgsConstructor
public class AuthorizationService {

    private final BoardRepository   boardRepository;
    private final ListRepository    listRepository;
    private final CardRepository    cardRepository;
    private final CommentRepository commentRepository;
    private final LabelRepository   labelRepository;

    /**
     * Check if current user is admin.
     */
    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    /**
     * Check if current user is moderator.
     */
    public boolean isModerator() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MODERATOR"));
    }

    /**
     * Check if current user is admin or moderator.
     */
    public boolean isAdminOrModerator() {
        return isAdmin() || isModerator();
    }

    /**
     * Check if user owns the board or is admin.
     */
    @Transactional(readOnly = true)
    public boolean canAccessBoard(Long boardId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        // Admins can access everything
        if (isAdmin()) {
            return true;
        }

        UserPrincipal user = (UserPrincipal) auth.getPrincipal();
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", boardId));

        // Board owner can access
        if (board.getOwner() != null && board.getOwner().getId().equals(user.getId())) {
            return true;
        }

        // For now, allow all authenticated users to access boards
        // In the future, you might want to implement board members/collaborators
        return true;
    }

    /**
     * Check if user can modify the board (owner or admin).
     */
    @Transactional(readOnly = true)
    public boolean canModifyBoard(Long boardId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        // Admins can modify everything
        if (isAdmin()) {
            return true;
        }

        UserPrincipal user = (UserPrincipal) auth.getPrincipal();
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", boardId));

        // Only board owner can modify
        return board.getOwner() != null && board.getOwner().getId().equals(user.getId());
    }

    /**
     * Check if user can delete the board (owner or admin).
     */
    @Transactional(readOnly = true)
    public boolean canDeleteBoard(Long boardId) {
        return canModifyBoard(boardId);
    }

    /**
     * Check if user can access a list.
     */
    @Transactional(readOnly = true)
    public boolean canAccessList(Long listId) {
        BoardList list = listRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List", "id", listId));
        return canAccessBoard(list.getBoard().getId());
    }

    /**
     * Check if user can modify a list.
     */
    @Transactional(readOnly = true)
    public boolean canModifyList(Long listId) {
        BoardList list = listRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List", "id", listId));
        return canModifyBoard(list.getBoard().getId());
    }

    /**
     * Check if user can access a card.
     */
    @Transactional(readOnly = true)
    public boolean canAccessCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id", cardId));
        return canAccessBoard(card.getList().getBoard().getId());
    }

    /**
     * Check if user can modify a card.
     */
    @Transactional(readOnly = true)
    public boolean canModifyCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id", cardId));
        return canModifyBoard(card.getList().getBoard().getId());
    }

    /**
     * Get current authenticated user ID.
     */
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated");
        }

        if (auth.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) auth.getPrincipal()).getId();
        }

        throw new AccessDeniedException("Invalid authentication principal");
    }

    /**
     * Get current authenticated user principal.
     */
    public UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated");
        }

        if (auth.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) auth.getPrincipal();
        }

        throw new AccessDeniedException("Invalid authentication principal");
    }

    /**
     * Check if user can modify a label (delegates to board-level permission).
     */
    @Transactional(readOnly = true)
    public boolean canModifyLabel(Long labelId) {
        Label label = labelRepository.findByIdWithBoard(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label", "id", labelId));
        return canModifyBoard(label.getBoard().getId());
    }

    /**
     * Check if current user can modify (edit/delete) a comment.
     * Admins and the original author are allowed.
     */
    @Transactional(readOnly = true)
    public boolean canModifyComment(Long commentId) {
        if (isAdmin()) return true;
        UserPrincipal user = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
        return comment.getAuthor() != null
                && comment.getAuthor().getId().equals(user.getId());
    }

    /**
     * Verify and throw exception if user cannot access board.
     */
    public void requireBoardAccess(Long boardId) {
        if (!canAccessBoard(boardId)) {
            throw new AccessDeniedException("You do not have permission to access this board");
        }
    }

    /**
     * Verify and throw exception if user cannot modify board.
     */
    public void requireBoardModification(Long boardId) {
        if (!canModifyBoard(boardId)) {
            throw new AccessDeniedException("You do not have permission to modify this board");
        }
    }
}

