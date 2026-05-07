package com.taskboard.service;
import com.taskboard.exception.ResourceNotFoundException;
import com.taskboard.messaging.producer.EventPublisher;
import com.taskboard.model.dto.CommentDTO;
import com.taskboard.model.dto.CreateCommentRequest;
import com.taskboard.model.entity.ActivityType;
import com.taskboard.model.entity.Card;
import com.taskboard.model.entity.Comment;
import com.taskboard.model.entity.User;
import com.taskboard.model.event.CommentAddedEvent;
import com.taskboard.repository.CardRepository;
import com.taskboard.repository.CommentRepository;
import com.taskboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * Service for card comment operations.
 *
 * Cache strategy
 * Comments are cached under "comments::{cardId}" independently of boards.
 * Adding, editing or deleting a comment evicts ONLY "comments::{cardId}".
 * The "boards" cache is NEVER invalidated, so board-list performance is unchanged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final EventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;
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

        // Log activity (DB write — stays inside transaction)
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("comment_id", comment.getId());
        metadata.put("author", author.getUsername());
        activityLogService.logActivity(
                card.getBoard(), author, ActivityType.COMMENT_ADDED,
                String.format("'%s' commented on card '%s'", author.getUsername(), card.getTitle()),
                metadata);

        // Publish event and WebSocket update after DB commit to prevent dual-write
        final Comment savedComment = comment;
        final Card savedCard = card;
        final Long boardId = card.getBoard().getId();
        final CommentDTO commentDTO = toDTO(comment);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishCommentAddedEvent(savedComment, savedCard);
                sendWebSocketUpdate(boardId, cardId, "COMMENT_ADDED", commentDTO);
            }
        });

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
        org.springframework.cache.Cache commentsCache = cacheManager.getCache("comments");
        if (commentsCache != null) {
            commentsCache.evict(cardId);
        }
        log.info("Updated comment {} on card {}", commentId, comment.getCard().getId());
        sendWebSocketUpdate(
                comment.getCard().getBoard().getId(),
                comment.getCard().getId(),
                "COMMENT_UPDATED",
                toDTO(comment));
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
        Long cardId  = comment.getCard().getId();
        Long boardId = comment.getCard().getBoard().getId();
        commentRepository.delete(comment);
        evictCommentsCache(cardId);
        log.info("Deleted comment {} on card {}", commentId, cardId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("commentId", commentId);
        payload.put("cardId",    cardId);
        sendWebSocketUpdate(boardId, cardId, "COMMENT_DELETED", payload);
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
    private void publishCommentAddedEvent(Comment comment, Card card) {
        String preview = comment.getContent().length() > 100
                ? comment.getContent().substring(0, 100) + "..."
                : comment.getContent();

        eventPublisher.publishCommentAdded(CommentAddedEvent.builder()
                .commentId(comment.getId())
                .cardId(card.getId())
                .cardTitle(card.getTitle())
                .boardId(card.getBoard().getId())
                .boardName(card.getBoard().getName())
                .authorId(comment.getAuthor().getId())
                .authorUsername(comment.getAuthor().getUsername())
                .contentPreview(preview)
                .timestamp(LocalDateTime.now())
                .build());
    }
    private void sendWebSocketUpdate(Long boardId, Long cardId, String eventType, Object data) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type",
                    eventType);
            message.put("data",      data);
            message.put("cardId",    cardId);
            message.put("timestamp", LocalDateTime.now());
            messagingTemplate.convertAndSend("/topic/board/" + boardId, (Object) message);
            messagingTemplate.convertAndSend("/topic/card/"  + cardId,  (Object) message);
        } catch (Exception e) {
            log.error("Failed to send WebSocket update for comment event: {}", e.getMessage());
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
