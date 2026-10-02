package com.taskboard.service;

import com.taskboard.model.entity.ActivityType;
import com.taskboard.model.entity.Board;
import com.taskboard.model.entity.User;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Map;

/**
 * Describes one activity to be logged.
 *
 * <p>Built fluently so callers no longer assemble a metadata {@code HashMap} by hand:
 * <pre>{@code
 * activityLogService.record(ActivityRecord.builder()
 *         .board(board)
 *         .user(user)
 *         .type(ActivityType.CARD_CREATED)
 *         .description(String.format("Card '%s' was created", card.getTitle()))
 *         .detail("card_title", card.getTitle())
 *         .build());
 * }</pre>
 *
 * @see ActivityLogService#record(ActivityRecord)
 */
@Getter
@Builder
public class ActivityRecord {

    private final Board board;

    /** The acting user, or {@code null} for system-initiated activity. */
    private final User user;

    private final ActivityType type;

    private final String description;

    /** Structured metadata, accumulated via {@code .detail(key, value)}. */
    @Singular("detail")
    private final Map<String, Object> details;
}
