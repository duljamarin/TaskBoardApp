package com.taskboard.security;

import com.taskboard.exception.ResourceNotFoundException;
import com.taskboard.model.entity.BoardMemberRole;
import com.taskboard.model.entity.Comment;
import com.taskboard.repository.BoardMemberRepository;
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

import java.util.Optional;

/**
 * Service for authorization checks.
 * Uses lightweight projection queries to avoid loading full entity graphs
 * during permission checks (e.g., only fetches owner_id instead of the whole Board).
 */
@Slf4j
@Service("authorizationService")
@RequiredArgsConstructor
public class AuthorizationService {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final ListRepository listRepository;
    private final CardRepository cardRepository;
    private final CommentRepository commentRepository;
    private final LabelRepository labelRepository;

    private static final SimpleGrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");
    private static final SimpleGrantedAuthority ROLE_MODERATOR = new SimpleGrantedAuthority("ROLE_MODERATOR");

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().contains(ROLE_ADMIN);
    }

    public boolean isModerator() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().contains(ROLE_MODERATOR);
    }

    public boolean isAdminOrModerator() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().contains(ROLE_ADMIN)
                || auth.getAuthorities().contains(ROLE_MODERATOR);
    }

    /**
     * Check if the current user can access (read) a board.
     * Any board member can access.
     */
    @Transactional(readOnly = true)
    public boolean canAccessBoard(Long boardId) {
        return hasPermissionOnBoard(boardId);
    }

    /**
     * Check if the current user can modify board content (lists, cards).
     * Requires OWNER or EDITOR role.
     */
    @Transactional(readOnly = true)
    public boolean canModifyBoard(Long boardId) {
        return hasRoleOnBoard(boardId, BoardMemberRole.EDITOR);
    }

    /**
     * Check if the current user can delete (archive) a board.
     * Requires OWNER role.
     */
    @Transactional(readOnly = true)
    public boolean canDeleteBoard(Long boardId) {
        return hasRoleOnBoard(boardId, BoardMemberRole.OWNER);
    }

    /**
     * Check if user can access a list — resolves list -> board with a single scalar query.
     */
    @Transactional(readOnly = true)
    public boolean canAccessList(Long listId) {
        Long boardId = listRepository.findBoardIdByListId(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List", "id", listId));
        return hasPermissionOnBoard(boardId);
    }

    /**
     * Check if user can modify a list — requires EDITOR or OWNER role.
     */
    @Transactional(readOnly = true)
    public boolean canModifyList(Long listId) {
        Long boardId = listRepository.findBoardIdByListId(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List", "id", listId));
        return hasRoleOnBoard(boardId, BoardMemberRole.EDITOR);
    }

    /**
     * Check if user can access a card — resolves card -> board with a single scalar query.
     */
    @Transactional(readOnly = true)
    public boolean canAccessCard(Long cardId) {
        Long boardId = cardRepository.findBoardIdByCardId(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id", cardId));
        return hasPermissionOnBoard(boardId);
    }

    /**
     * Check if user can modify a card — requires EDITOR or OWNER role.
     */
    @Transactional(readOnly = true)
    public boolean canModifyCard(Long cardId) {
        Long boardId = cardRepository.findBoardIdByCardId(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id", cardId));
        return hasRoleOnBoard(boardId, BoardMemberRole.EDITOR);
    }

    /**
     * Check if user can modify a label — requires EDITOR or OWNER role.
     */
    @Transactional(readOnly = true)
    public boolean canModifyLabel(Long labelId) {
        var label = labelRepository.findByIdWithBoard(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label", "id", labelId));
        return hasRoleOnBoard(label.getBoard().getId(), BoardMemberRole.EDITOR);
    }

    @Transactional(readOnly = true)
    public boolean canModifyComment(Long commentId) {
        if (isAdmin()) return true;
        UserPrincipal user = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
        return comment.getAuthor() != null
                && comment.getAuthor().getId().equals(user.getId());
    }

    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated");
        }
        if (auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        throw new AccessDeniedException("Invalid authentication principal");
    }

    public UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated");
        }
        if (auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Invalid authentication principal");
    }

    /**
     * Overload for WebSocket handlers where SecurityContextHolder is not populated.
     */
    @Transactional(readOnly = true)
    public boolean canAccessBoard(Long boardId, UserPrincipal user) {
        if (user.getAuthorities().contains(ROLE_ADMIN)
                || user.getAuthorities().contains(ROLE_MODERATOR)) {
            return true;
        }
        return boardMemberRepository.existsByBoardIdAndUserId(boardId, user.getId());
    }

    public void requireBoardAccess(Long boardId) {
        if (!canAccessBoard(boardId)) {
            throw new AccessDeniedException("You do not have permission to access this board");
        }
    }

    public void requireBoardAccess(Long boardId, UserPrincipal user) {
        if (!canAccessBoard(boardId, user)) {
            throw new AccessDeniedException("You do not have permission to access this board");
        }
    }

    public void requireBoardModification(Long boardId) {
        if (!canModifyBoard(boardId)) {
            throw new AccessDeniedException("You do not have permission to modify this board");
        }
    }

    /**
     * Core permission check: admin/moderator pass immediately,
     * otherwise check board_members for the current user.
     */
    private boolean hasPermissionOnBoard(Long boardId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        if (auth.getAuthorities().contains(ROLE_ADMIN)
                || auth.getAuthorities().contains(ROLE_MODERATOR)) {
            return true;
        }
        UserPrincipal user = (UserPrincipal) auth.getPrincipal();
        return boardMemberRepository.existsByBoardIdAndUserId(boardId, user.getId());
    }

    /**
     * Check if the current user has at least the given role on the board.
     * Role hierarchy: OWNER > EDITOR > MEMBER.
     * Admin/moderator bypass all checks.
     */
    private boolean hasRoleOnBoard(Long boardId, BoardMemberRole minimumRole) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        if (auth.getAuthorities().contains(ROLE_ADMIN)
                || auth.getAuthorities().contains(ROLE_MODERATOR)) {
            return true;
        }
        UserPrincipal user = (UserPrincipal) auth.getPrincipal();
        Optional<BoardMemberRole> role = boardMemberRepository.findRoleByBoardIdAndUserId(boardId, user.getId());
        return role.map(r -> meetsMinimumRole(r, minimumRole)).orElse(false);
    }

    private static boolean meetsMinimumRole(BoardMemberRole actual, BoardMemberRole required) {
        return actual.ordinal() <= required.ordinal();
    }
}
