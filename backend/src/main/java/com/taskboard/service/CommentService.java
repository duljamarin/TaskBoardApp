package com.taskboard.service;

import com.taskboard.exception.ResourceNotFoundException;
import com.taskboard.model.dto.CommentDTO;
import com.taskboard.model.dto.CreateCommentRequest;
import com.taskboard.model.entity.ActivityType;
import com.taskboard.model.entity.Card;
import com.taskboard.model.entity.Comment;
import com.taskboard.model.entity.User;
import com.taskboard.repository.CardRepository;
import com.taskboard.repository.CommentRepository;
import com.taskboard.repository.UserRepository;
import com.taskboard.service.event.CommentAddedAppEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for card comment operations.
 *
 * Cache strategy: Comments are cached under "comments::{cardId}" independently of boards.
 * Adding, editing or deleting a comment evicts ONLY "comments::{cardId}".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final CacheManager cacheManager;

    @Cacheable(value = "comments", key = "#cardId")
    @Transactional(readOnly = true)
    public List<CommentDTO> getCommentsByCardId(Long cardId) {
        log.debug("Fetching comments for card {} from database", cardId);
        if (!cardRepository.existsById(cardId)) {
            throw new ResourceNotFoundException("Card", "id", cardId);
        }
        return commentRepository.findByCardIdWithAuthor(cardId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "comments", key = "#cardId")
    @Transactional
    public CommentDTO addComment(Long cardId, CreateCommentRequest request, Long authorId) {
        log.info("Adding comment to card {} by user {}", cardId, authorId);
        Card card = cardRepository.findByIdWithDetails(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id", cardId));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", authorId));

        Comment comment = Comment.builder()
                .card(card)
                .author(author)
                .content(request.getContent())
                .build();
        comment = commentRepository.save(comment);
        log.info("Created comment {} on card '{}'", comment.getId(), card.getTitle());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("comment_id", comment.getId());
        metadata.put("author", author.getUsername());
        activityLogService.logActivity(
                card.getBoard(), author, ActivityType.COMMENT_ADDED,
                String.format("'%s' commented on card '%s'", author.getUsername(), card.getTitle()),
                metadata);

        String preview = comment.getContent().length() > 100
                ? comment.getContent().substring(0, 100) + "..."
                : comment.getContent();

        CommentDTO commentDTO = toDTO(comment);
        eventPublisher.publishEvent(CommentAddedAppEvent.builder()
                .commentId(comment.getId())
                .cardId(card.getId())
                .cardTitle(card.getTitle())
                .boardId(card.getBoard().getId())
                .boardName(card.getBoard().getName())
                .authorId(author.getId())
                .authorUsername(author.getUsername())
                .contentPreview(preview)
                .commentDTO(commentDTO)
                .build());

        return commentDTO;
    }

    @Transactional
    public CommentDTO updateComment(Long commentId, CreateCommentRequest request, Long requestingUserId) {
        Comment comment = findCommentOrThrow(commentId);
        requireAuthor(comment, requestingUserId);
        Long cardId = comment.getCard().getId();
        comment.setContent(request.getContent());
        comment.setEdited(true);
        comment = commentRepository.save(comment);
        evictCommentsCache(cardId);
        log.info("Updated comment {} on card {}", commentId, comment.getCard().getId());
        return toDTO(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, Long requestingUserId, boolean isAdmin) {
        Comment comment = findCommentOrThrow(commentId);
        boolean isAuthor = comment.getAuthor() != null
                && comment.getAuthor().getId().equals(requestingUserId);
        if (!isAuthor && !isAdmin) {
            throw new AccessDeniedException("You can only delete your own comments");
        }
        Long cardId = comment.getCard().getId();
        commentRepository.delete(comment);
        evictCommentsCache(cardId);
        log.info("Deleted comment {} on card {}", commentId, cardId);
    }

    @CacheEvict(value = "comments", key = "#cardId")
    public void evictCommentsCache(Long cardId) { /* Spring AOP handles eviction */ }

    private Comment findCommentOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
    }

    private void requireAuthor(Comment comment, Long userId) {
        if (comment.getAuthor() == null || !comment.getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("You can only edit your own comments");
        }
    }

    private CommentDTO toDTO(Comment comment) {
        return CommentDTO.builder()
                .id(comment.getId())
                .cardId(comment.getCard().getId())
                .authorId(comment.getAuthor() != null ? comment.getAuthor().getId() : null)
                .authorUsername(comment.getAuthor() != null ? comment.getAuthor().getUsername() : null)
                .authorFullName(comment.getAuthor() != null ? comment.getAuthor().getFullName() : null)
                .content(comment.getContent())
                .edited(comment.getEdited())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
