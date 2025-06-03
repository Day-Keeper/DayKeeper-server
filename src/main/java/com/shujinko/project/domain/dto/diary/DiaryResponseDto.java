package com.shujinko.project.domain.dto.diary;

import com.shujinko.project.domain.dto.ai.EmotionResultDto;
import com.shujinko.project.domain.dto.ai.EmotionScoreDto;
import com.shujinko.project.domain.dto.ai.aiKeywordDto;
import com.shujinko.project.domain.entity.diary.DiaryEmotion;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class DiaryResponseDto {
    private String rawDiary;
    private String rephrasedDiary;
    private LocalDateTime createdAt;
    private String summary;
    private String label;
    private List<aiKeywordDto> keywords;
    private List<EmotionScoreDto> emotions;
    private Long diaryId;
}
