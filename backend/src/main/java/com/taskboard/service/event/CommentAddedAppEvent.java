package com.taskboard.service.event;

import com.taskboard.model.dto.CommentDTO;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentAddedAppEvent {
    private final Long commentId;
    private final Long cardId;
    private final String cardTitle;
    private final Long boardId;
    private final String boardName;
    private final Long authorId;
    private final String authorUsername;
    private final String contentPreview;
    private final CommentDTO commentDTO;
}
