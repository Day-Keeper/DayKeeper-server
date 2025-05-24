package com.shujinko.project.domain.dto.ai;

import lombok.*;

import java.util.List;
import java.util.Map;
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class EmotionResultDto {
    private String label;
    private List<EmotionScoreDto> emotionScores;
}
