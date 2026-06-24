package com.taskboard.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Utility for deferring side-effects (RabbitMQ publishes, WebSocket pushes)
 * until after the current transaction commits. Prevents ghost events
 * if the transaction rolls back.
 */
public final class TransactionHooks {

    private TransactionHooks() {}

    /**
     * Run the given action after the current transaction commits successfully.
     * If no transaction is active, the action runs immediately.
     */
    public static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
