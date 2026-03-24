package com.taskboard.controller;

import com.taskboard.messaging.consumer.AnalyticsConsumer;
import com.taskboard.service.ListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * REST controller for analytics and metrics.
 * Exposes collected metrics from the AnalyticsConsumer.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsConsumer analyticsConsumer;
    private final ListService listService;

    /**
     * Get all analytics metrics.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllMetrics() {
        log.debug("Fetching all analytics metrics");

        ConcurrentHashMap<String, AtomicLong> rawMetrics = analyticsConsumer.getAllMetrics();
        Map<String, Long> metrics = new HashMap<>();
        rawMetrics.forEach((key, value) -> metrics.put(key, value.get()));

        Map<String, Object> response = new HashMap<>();
        response.put("metrics", metrics);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * Get overview metrics (summary statistics).
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        log.debug("Fetching analytics overview");

        Map<String, Object> overview = new HashMap<>();

        overview.put("totalCardsCreated", analyticsConsumer.getMetric("cards_created_total"));
        overview.put("totalCardsMoved", analyticsConsumer.getMetric("cards_moved_total"));
        overview.put("totalBoardsCreated", analyticsConsumer.getMetric("boards_created_total"));
        overview.put("totalListsCreated", listService.countAllLists());

        long totalCards = analyticsConsumer.getMetric("cards_created_total");
        long totalMoves = analyticsConsumer.getMetric("cards_moved_total");
        double avgMovesPerCard = totalCards > 0 ? (double) totalMoves / totalCards : 0;
        overview.put("avgMovesPerCard", Math.round(avgMovesPerCard * 100.0) / 100.0);

        overview.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(overview);
    }

    /**
     * Get card-specific metrics.
     */
    @GetMapping("/cards")
    public ResponseEntity<Map<String, Object>> getCardMetrics() {
        log.debug("Fetching card analytics");

        Map<String, Object> cardMetrics = new HashMap<>();

        cardMetrics.put("totalCreated", analyticsConsumer.getMetric("cards_created_total"));
        cardMetrics.put("totalMoved", analyticsConsumer.getMetric("cards_moved_total"));

        Map<String, Long> priorityDistribution = new HashMap<>();
        priorityDistribution.put("low", analyticsConsumer.getMetric("cards_created_priority_low"));
        priorityDistribution.put("medium", analyticsConsumer.getMetric("cards_created_priority_medium"));
        priorityDistribution.put("high", analyticsConsumer.getMetric("cards_created_priority_high"));
        cardMetrics.put("priorityDistribution", priorityDistribution);

        cardMetrics.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(cardMetrics);
    }

    /**
     * Get board-specific metrics.
     */
    @GetMapping("/boards")
    public ResponseEntity<Map<String, Object>> getBoardMetrics() {
        log.debug("Fetching board analytics");

        Map<String, Object> boardMetrics = new HashMap<>();
        boardMetrics.put("totalCreated", analyticsConsumer.getMetric("boards_created_total"));
        boardMetrics.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(boardMetrics);
    }

    /**
     * Reset all in-memory metrics.
     *
     * Supports both DELETE and GET for backward compatibility.
     */
    @RequestMapping(value = "/reset", method = {RequestMethod.DELETE, RequestMethod.GET})
    public ResponseEntity<Map<String, String>> resetMetrics() {
        log.warn("Resetting all analytics metrics");

        analyticsConsumer.getAllMetrics().clear();

        Map<String, String> response = new HashMap<>();
        response.put("message", "All metrics have been reset");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return ResponseEntity.ok(response);
    }
}

