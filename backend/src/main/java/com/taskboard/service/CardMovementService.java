package com.taskboard.service;

import com.taskboard.exception.ResourceNotFoundException;
import com.taskboard.model.dto.CardDTO;
import com.taskboard.model.dto.CardMapper;
import com.taskboard.model.dto.CardMoveDTO;
import com.taskboard.model.entity.*;
import com.taskboard.repository.CardRepository;
import com.taskboard.repository.ListRepository;
import com.taskboard.repository.UserRepository;
import com.taskboard.service.event.CardMovedAppEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

/**
 * Service for card movement operations.
 * Handles the complex logic of moving cards between lists with proper position management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardMovementService {

    private final CardRepository cardRepository;
    private final ListRepository listRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final ApplicationEventPublisher eventPublisher;

    @CacheEvict(value = "boards", allEntries = true)
    @Transactional
    public CardDTO moveCard(Long cardId, CardMoveDTO moveDTO, Long userId) {
        log.info("Moving card {} to list {} at position {} by user: {}",
                cardId, moveDTO.getNewListId(), moveDTO.getNewPosition(), userId);

        Card card = cardRepository.findByIdWithDetails(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id", cardId));

        BoardList newList = listRepository.findById(moveDTO.getNewListId())
                .orElseThrow(() -> new ResourceNotFoundException("List", "id", moveDTO.getNewListId()));

        User mover = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        validateMove(card, newList);

        lockLists(card.getList().getId(), newList.getId());

        Long oldListId = card.getList().getId();
        String oldListName = card.getList().getName();
        Integer oldPosition = card.getPosition();

        if (oldListId.equals(moveDTO.getNewListId())) {
            moveWithinSameList(card, moveDTO.getNewPosition());
        } else {
            moveToDifferentList(card, newList, moveDTO.getNewPosition());
        }

        card = cardRepository.save(card);
        log.info("Moved card '{}' from '{}' to '{}'", card.getTitle(), oldListName, newList.getName());

        logCardMoved(card, oldListName, mover);

        CardDTO cardDTO = CardMapper.toDTO(card);
        eventPublisher.publishEvent(CardMovedAppEvent.builder()
                .cardId(card.getId())
                .cardTitle(card.getTitle())
                .boardId(card.getBoard().getId())
                .boardName(card.getBoard().getName())
                .fromListId(oldListId)
                .fromListName(oldListName)
                .fromPosition(oldPosition)
                .toListId(card.getList().getId())
                .toListName(card.getList().getName())
                .toPosition(card.getPosition())
                .movedByUserId(mover.getId())
                .movedByUsername(mover.getUsername())
                .cardDTO(cardDTO)
                .build());

        return cardDTO;
    }

    /**
     * Lock the affected list rows, always in ascending id order, to prevent deadlocks
     * between concurrent moves touching the same pair of lists.
     */
    private void lockLists(Long... listIds) {
        Stream.of(listIds).distinct().sorted().forEach(id ->
                listRepository.findByIdForUpdate(id)
                        .orElseThrow(() -> new ResourceNotFoundException("List", "id", id)));
    }

    private void validateMove(Card card, BoardList targetList) {
        if (!card.getList().getBoard().getId().equals(targetList.getBoard().getId())) {
            throw new IllegalArgumentException("Cannot move card to a list on a different board");
        }
    }

    private void moveWithinSameList(Card card, Integer newPosition) {
        Integer oldPosition = card.getPosition();
        Long listId = card.getList().getId();

        if (oldPosition < newPosition) {
            cardRepository.decrementPositionsAfter(listId, oldPosition);
            cardRepository.incrementPositionsFrom(listId, newPosition);
        } else if (oldPosition > newPosition) {
            cardRepository.incrementPositionsFrom(listId, newPosition);
            cardRepository.decrementPositionsAfter(listId, oldPosition);
        }

        card.setPosition(newPosition);
    }

    private void moveToDifferentList(Card card, BoardList newList, Integer newPosition) {
        Long oldListId = card.getList().getId();
        Integer oldPosition = card.getPosition();

        cardRepository.decrementPositionsAfter(oldListId, oldPosition);
        cardRepository.incrementPositionsFrom(newList.getId(), newPosition);

        card.setList(newList);
        card.setPosition(newPosition);
    }

    private void logCardMoved(Card card, String fromListName, User mover) {
        activityLogService.record(ActivityRecord.builder()
                .board(card.getBoard())
                .user(mover)
                .type(ActivityType.CARD_MOVED)
                .description(String.format("Card '%s' was moved from '%s' to '%s' by %s",
                        card.getTitle(), fromListName, card.getList().getName(), mover.getUsername()))
                .detail("card_title", card.getTitle())
                .detail("from_list", fromListName)
                .detail("to_list", card.getList().getName())
                .detail("moved_by", mover.getUsername())
                .build());
    }
}
