package com.taskboard.service.event;

import com.taskboard.model.dto.CardDTO;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardUpdatedAppEvent {
    private final Long boardId;
    private final CardDTO cardDTO;
}
