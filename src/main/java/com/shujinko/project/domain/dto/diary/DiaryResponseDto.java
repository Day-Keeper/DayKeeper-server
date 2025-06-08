package com.shujinko.project.domain.dto.diary;

import com.shujinko.project.domain.dto.ai.EmotionScoreDto;
import com.shujinko.project.domain.dto.ai.ParagraphDto;
import com.shujinko.project.domain.dto.ai.aiKeywordDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class DiaryResponseDto {
    private String rawDiary;
    private String rephrasedDiary;
    private List<ParagraphDto> paragraph;
    private LocalDateTime createdAt;
    private String summary;
    private String label;
    private List<aiKeywordDto> keywords;
    private List<EmotionScoreDto> emotions;
    private Long diaryId;
}
