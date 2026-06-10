package com.minimalhealth.service;

import com.minimalhealth.exception.BusinessException;
import com.minimalhealth.model.dto.request.FinishExerciseRequest;
import com.minimalhealth.model.dto.response.ExerciseListResponse;
import com.minimalhealth.model.dto.response.ExerciseListResponse.*;
import com.minimalhealth.model.entity.*;
import com.minimalhealth.model.enums.ActivityType;
import com.minimalhealth.model.enums.ExerciseStatus;
import com.minimalhealth.repository.*;
import com.minimalhealth.util.HealthScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRecordRepository exerciseRecordRepository;
    private final DailyStatsRepository dailyStatsRepository;
    private final ActivityLogRepository activityLogRepository;
    private final HealthGoalRepository healthGoalRepository;
    private final UserRepository userRepository;

    public ExerciseListResponse getExercises(Long userId, LocalDate weekStart) {
        if (weekStart == null) {
            weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        }
        LocalDate weekEnd = weekStart.plusDays(6);

        List<ExerciseRecord> records = exerciseRecordRepository
            .findByUserIdAndRecordDateBetweenOrderByRecordDateDesc(userId, weekStart, weekEnd);
        Integer count = exerciseRecordRepository.countByUserIdAndDateBetween(userId, weekStart, weekEnd);
        Double totalDist = exerciseRecordRepository.sumDistanceByUserIdAndDateBetween(userId, weekStart, weekEnd);
        Integer totalCal = exerciseRecordRepository.sumCaloriesByUserIdAndDateBetween(userId, weekStart, weekEnd);

        return ExerciseListResponse.builder()
            .weeklyStats(WeeklyStats.builder()
                .count(count != null ? count : 0)
                .totalDistanceKm(totalDist != null ? totalDist : 0.0)
                .totalCalories(totalCal != null ? totalCal : 0)
                .build())
            .records(records.stream().map(r -> ExerciseItem.builder()
                .id(r.getId()).exerciseType(r.getExerciseType())
                .durationMin(r.getDurationMin()).distanceKm(r.getDistanceKm())
                .calories(r.getCalories()).avgPace(r.getAvgPace())
                .routeDesc(r.getRouteDesc())
                .recordDate(r.getRecordDate().toString())
                .status(r.getStatus().name())
                .build()).collect(Collectors.toList()))
            .build();
    }

    @Transactional
    public Map<String, Object> startExercise(Long userId, String exerciseType) {
        // Check if there's already an active session
        exerciseRecordRepository.findByUserIdAndStatus(userId, ExerciseStatus.in_progress)
            .ifPresent(e -> { throw new BusinessException("已有进行中的运动"); });

        User user = userRepository.findById(userId).orElseThrow();
        ExerciseRecord record = ExerciseRecord.builder()
            .user(user)
            .exerciseType(exerciseType)
            .recordDate(LocalDate.now())
            .startedAt(LocalDateTime.now())
            .status(ExerciseStatus.in_progress)
            .build();
        exerciseRecordRepository.save(record);

        return Map.of("sessionId", record.getId(), "startedAt", record.getStartedAt().toString());
    }

    @Transactional
    public ExerciseItem finishExercise(Long userId, Long exerciseId, FinishExerciseRequest req) {
        ExerciseRecord record = exerciseRecordRepository.findById(exerciseId)
            .orElseThrow(() -> new BusinessException("运动记录不存在"));

        if (!record.getUser().getId().equals(userId)) {
            throw new BusinessException("无权操作");
        }
        if (record.getStatus() != ExerciseStatus.in_progress) {
            throw new BusinessException("运动已结束");
        }

        record.setDurationMin(req.getDurationMin());
        record.setDistanceKm(req.getDistanceKm());
        record.setCalories(req.getCalories());
        record.setAvgPace(req.getAvgPace());
        record.setAvgHr(req.getAvgHr());
        record.setRouteDesc(req.getRouteDesc());
        record.setStatus(ExerciseStatus.completed);
        record.setEndedAt(LocalDateTime.now());

        exerciseRecordRepository.save(record);

        // Update daily stats
        LocalDate today = LocalDate.now();
        DailyStats stats = dailyStatsRepository.findByUserIdAndRecordDate(userId, today)
            .orElseGet(() -> dailyStatsRepository.save(
                DailyStats.builder().user(userRepository.getReferenceById(userId)).recordDate(today).build()
            ));
        if (req.getDistanceKm() != null) {
            int steps = (int) (req.getDistanceKm() * 1300);
            stats.setSteps((stats.getSteps() != null ? stats.getSteps() : 0) + steps);
        }
        if (req.getCalories() != null) {
            stats.setCalorieBurned((stats.getCalorieBurned() != null ? stats.getCalorieBurned() : 0) + req.getCalories());
        }
        HealthGoal goal = healthGoalRepository.findByUserId(userId).orElse(null);
        if (goal != null) stats.setHealthScore(HealthScoreCalculator.compute(stats, goal));
        dailyStatsRepository.save(stats);

        // Activity log
        String detail = "";
        if (req.getDistanceKm() != null) detail += req.getDistanceKm() + "km";
        if (req.getRouteDesc() != null) detail += " · " + req.getRouteDesc();
        activityLogRepository.save(ActivityLog.builder()
            .user(userRepository.getReferenceById(userId))
            .activityType(ActivityType.exercise)
            .name(record.getExerciseType()).detail(detail.trim())
            .valueLabel(req.getCalories() + " kcal").valueAmount(req.getCalories() != null ? req.getCalories().doubleValue() : 0.0)
            .valueUnit("kcal").occurredAt(LocalDateTime.now()).recordDate(today)
            .build());

        return ExerciseItem.builder()
            .id(record.getId()).exerciseType(record.getExerciseType())
            .durationMin(record.getDurationMin()).distanceKm(record.getDistanceKm())
            .calories(record.getCalories()).avgPace(record.getAvgPace())
            .routeDesc(record.getRouteDesc())
            .recordDate(record.getRecordDate().toString())
            .status(record.getStatus().name())
            .build();
    }

    public ExerciseItem getRunningStatus(Long userId) {
        return exerciseRecordRepository.findByUserIdAndStatus(userId, ExerciseStatus.in_progress)
            .map(r -> ExerciseItem.builder()
                .id(r.getId()).exerciseType(r.getExerciseType())
                .durationMin(r.getDurationMin()).distanceKm(r.getDistanceKm())
                .calories(r.getCalories()).avgPace(r.getAvgPace())
                .routeDesc(r.getRouteDesc())
                .recordDate(r.getRecordDate().toString()).status(r.getStatus().name())
                .build())
            .orElse(null);
    }
}
