package com.taskboard.service.event;

import com.taskboard.model.dto.CardDTO;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardMovedAppEvent {
    private final Long cardId;
    private final String cardTitle;
    private final Long boardId;
    private final String boardName;
    private final Long fromListId;
    private final String fromListName;
    private final Integer fromPosition;
    private final Long toListId;
    private final String toListName;
    private final Integer toPosition;
    private final Long movedByUserId;
    private final String movedByUsername;
    private final CardDTO cardDTO;
}
