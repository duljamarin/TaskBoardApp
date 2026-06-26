package com.taskboard.model.entity;

/**
 * Roles a user can hold within a board.
 * OWNER — full control (delete board, manage members).
 * EDITOR — modify board content (lists, cards).
 * MEMBER — read access + card assignment.
 */
public enum BoardMemberRole {
    OWNER,
    EDITOR,
    MEMBER
}
