package com.shujinko.project.domain.dto.ai;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class EmotionScoreDto {
    private String emotion;
    private Double score;
}
