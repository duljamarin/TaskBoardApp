package com.taskboard.service.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardDeletedAppEvent {
    private final Long boardId;
    private final Long cardId;
    private final Long listId;
}
