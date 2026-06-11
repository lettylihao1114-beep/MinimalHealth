package com.minimalhealth.controller;

import com.minimalhealth.model.dto.request.UpdateDailyStatsRequest;
import com.minimalhealth.model.dto.response.ApiResponse;
import com.minimalhealth.model.entity.DailyStats;
import com.minimalhealth.model.entity.User;
import com.minimalhealth.repository.DailyStatsRepository;
import com.minimalhealth.repository.UserRepository;
import com.minimalhealth.security.SecurityUtils;
import com.minimalhealth.util.HealthScoreCalculator;
import com.minimalhealth.model.entity.HealthGoal;
import com.minimalhealth.repository.HealthGoalRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final DailyStatsRepository dailyStatsRepository;
    private final UserRepository userRepository;
    private final HealthGoalRepository healthGoalRepository;

    @PutMapping
    public ApiResponse<Map<String, Object>> updateStats(@Valid @RequestBody UpdateDailyStatsRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDate today = LocalDate.now();

        DailyStats stats = dailyStatsRepository.findByUserIdAndRecordDate(userId, today)
            .orElseGet(() -> {
                User user = userRepository.getReferenceById(userId);
                return dailyStatsRepository.save(
                    DailyStats.builder().user(user).recordDate(today).build()
                );
            });

        if (request.getRestingHr() != null) {
            stats.setRestingHr(request.getRestingHr());
        }
        if (request.getSleepHours() != null) {
            stats.setSleepHours(request.getSleepHours());
        }

        // Recompute health score
        HealthGoal goal = healthGoalRepository.findByUserId(userId).orElse(null);
        if (goal != null) {
            stats.setHealthScore(HealthScoreCalculator.compute(stats, goal));
        }

        dailyStatsRepository.save(stats);

        return ApiResponse.success(Map.of(
            "restingHr", stats.getRestingHr(),
            "sleepHours", stats.getSleepHours(),
            "healthScore", stats.getHealthScore()
        ));
    }
}