package com.taskboard.controller;

import com.taskboard.model.dto.CardMoveDTO;
import com.taskboard.security.AuthorizationService;
import com.taskboard.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket controller for real-time board updates.
 * Handles STOMP messages for card movements and other real-time events.
 * All message handlers require authentication and board access.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final AuthorizationService authorizationService;

    /**
     * Handle card move messages from clients.
     * Broadcasts the move to all subscribers of the board topic.
     * Requires access to the board.
     */
    @MessageMapping("/board/{boardId}/card-move")
    @SendTo("/topic/board/{boardId}")
    public Map<String, Object> handleCardMove(
            @DestinationVariable Long boardId,
            CardMoveDTO moveDTO,
            Principal principal) {

        UserPrincipal user = extractUserPrincipal(principal);
        authorizationService.requireBoardAccess(boardId, user);

        log.info("WebSocket: Card move received for board {} from user {} - newListId: {}, newPosition: {}",
                boardId, user.getUsername(), moveDTO.getNewListId(), moveDTO.getNewPosition());

        Map<String, Object> response = new HashMap<>();
        response.put("type", "CARD_MOVE_BROADCAST");
        response.put("boardId", boardId);
        response.put("newListId", moveDTO.getNewListId());
        response.put("newPosition", moveDTO.getNewPosition());
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("timestamp", LocalDateTime.now());

        return response;
    }

    /**
     * Handle board subscription notifications.
     * Requires access to the board.
     */
    @MessageMapping("/board/{boardId}/subscribe")
    @SendTo("/topic/board/{boardId}")
    public Map<String, Object> handleBoardSubscription(
            @DestinationVariable Long boardId,
            Principal principal) {

        UserPrincipal user = extractUserPrincipal(principal);
        authorizationService.requireBoardAccess(boardId, user);

        log.info("WebSocket: User {} subscribed to board {}", user.getUsername(), boardId);

        Map<String, Object> response = new HashMap<>();
        response.put("type", "SUBSCRIPTION_ACK");
        response.put("boardId", boardId);
        response.put("message", "Successfully subscribed to board updates");
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("timestamp", LocalDateTime.now());

        return response;
    }

    /**
     * Extracts the UserPrincipal from the STOMP session principal.
     * Spring STOMP stores the UsernamePasswordAuthenticationToken (set during CONNECT)
     * as the session Principal, so we unwrap it here to get the actual UserPrincipal.
     */
    private UserPrincipal extractUserPrincipal(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }
        throw new AccessDeniedException("User not authenticated");
    }
}
