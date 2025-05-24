package com.shujinko.project.domain.dto.ai;

import lombok.*;

import java.util.Map;
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class aiEmotionDto {
    private String label;
    private Map<String,Double> scores;
}
