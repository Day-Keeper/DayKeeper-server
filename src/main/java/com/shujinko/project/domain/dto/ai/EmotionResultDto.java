package com.shujinko.project.domain.dto.ai;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class EmotionResultDto {
    private String label;
    private Map<String, Float> scores;
}
