package com.minimalhealth.model.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateDailyStatsRequest {
    @Min(value = 30, message = "心率范围 30~220")
    @Max(value = 220, message = "心率范围 30~220")
    private Integer restingHr;

    @DecimalMin(value = "0.0", message = "睡眠时长不能为负")
    @DecimalMax(value = "24.0", message = "睡眠时长不超过 24 小时")
    private Double sleepHours;
}