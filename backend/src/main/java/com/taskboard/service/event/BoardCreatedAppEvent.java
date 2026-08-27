package com.taskboard.service.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardCreatedAppEvent {
    private final Long boardId;
    private final String boardName;
    private final String description;
    private final String color;
    private final Long createdByUserId;
    private final String createdByUsername;
}
