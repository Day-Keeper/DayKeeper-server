package com.shujinko.project.domain.dto.ai;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class aiEmotionDto {
    private String label;
    private Map<String,Float> scores;
}
