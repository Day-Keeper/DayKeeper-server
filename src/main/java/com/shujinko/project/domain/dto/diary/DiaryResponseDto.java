package com.shujinko.project.domain.dto.diary;

import com.shujinko.project.domain.dto.ai.EmotionResultDto;
import com.shujinko.project.domain.dto.ai.EmotionScoreDto;
import com.shujinko.project.domain.entity.diary.DiaryEmotion;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DiaryResponseDto {
    private String rawDiary;
    private String rephrasedDiary;
    private LocalDateTime createdAt;
    private String summary;
    private String label;
    private List<String> keywords;
    private List<EmotionScoreDto> emotions;
}
