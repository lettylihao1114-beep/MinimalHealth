package com.minimalhealth.service;

import com.minimalhealth.model.entity.*;
import com.minimalhealth.repository.*;
import com.minimalhealth.util.GreetingUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiSuggestionService {

    private final AiSuggestionRepository aiSuggestionRepository;
    private final DailyStatsRepository dailyStatsRepository;
    private final HealthGoalRepository healthGoalRepository;
    private final UserRepository userRepository;

    public Map<String, Object> getSuggestions(Long userId, LocalDate dateParam) {
        final LocalDate date = dateParam != null ? dateParam : LocalDate.now();

        // Check if we already have suggestions for today
        List<AiSuggestion> existing = aiSuggestionRepository
            .findByUserIdAndGeneratedDateAndIsActiveTrueOrderByCreatedAtAsc(userId, date);
        if (!existing.isEmpty()) {
            return buildResponse(existing);
        }

        // Generate suggestions based on gaps
        DailyStats stats = dailyStatsRepository.findByUserIdAndRecordDate(userId, date).orElse(null);
        HealthGoal goal = healthGoalRepository.findByUserId(userId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        List<AiSuggestion> suggestions = new ArrayList<>();
        String userName = user != null && user.getName() != null ? user.getName() : "用户";

        if (stats != null && goal != null) {
            // Steps suggestion
            if (stats.getSteps() < goal.getDailySteps() * 0.8) {
                int remaining = goal.getDailySteps() - (stats.getSteps() != null ? stats.getSteps() : 0);
                suggestions.add(AiSuggestion.builder()
                    .user(user).title("步行目标")
                    .description("你今天还差 " + remaining + " 步即可达成目标。建议利用午餐后的步行时间，绕公司附近公园走一圈约15分钟即可完成。")
                    .iconKey("walking").generatedDate(date).isActive(true).build());
            }

            // Sleep suggestion
            if (stats.getSleepHours() < goal.getDailySleepH() * 0.8 && goal.getDailySleepH() > 0) {
                suggestions.add(AiSuggestion.builder()
                    .user(user).title("睡眠优化")
                    .description("过去几天你的睡眠时长偏低，建议今晚22:30前放下手机，营造安静睡眠环境。目标睡眠时长" + goal.getDailySleepH() + "小时。")
                    .iconKey("sleep").generatedDate(date).isActive(true).build());
            }

            // Hydration suggestion
            if (stats.getWaterMl() < goal.getDailyWaterMl() * 0.6) {
                int remaining = goal.getDailyWaterMl() - (stats.getWaterMl() != null ? stats.getWaterMl() : 0);
                suggestions.add(AiSuggestion.builder()
                    .user(user).title("补充水分")
                    .description("下午2-4点是补水的最佳时段，建议在接下来的2小时内补充" + (remaining / 100) * 100 + "ml水，保持身体最佳状态。")
                    .iconKey("water").generatedDate(date).isActive(true).build());
            }
        }

        // Default suggestion if none generated
        if (suggestions.isEmpty()) {
            suggestions.add(AiSuggestion.builder()
                .user(user).title("保持好状态")
                .description("你今天的各项指标表现不错！继续保持当前的生活习惯，健康的身体是最好的投资。")
                .iconKey("general").generatedDate(date).isActive(true).build());
        }

        aiSuggestionRepository.saveAll(suggestions);
        return buildResponse(suggestions);
    }

    private Map<String, Object> buildResponse(List<AiSuggestion> suggestions) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("greeting", GreetingUtils.getGreeting(LocalTime.now().getHour()));
        result.put("suggestions", suggestions.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("title", s.getTitle());
            m.put("description", s.getDescription());
            m.put("iconKey", s.getIconKey());
            return m;
        }).toList());
        return result;
    }
}
