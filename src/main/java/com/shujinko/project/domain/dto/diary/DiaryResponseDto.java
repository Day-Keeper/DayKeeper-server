package com.shujinko.project.domain.dto.diary;

import com.shujinko.project.domain.entity.diary.DiaryEmotion;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class DiaryResponseDto {
    private String rawDiary;
    private String rephrasedDiary;
    private LocalDateTime createdAt;
    private String summary;
    private List<KeywordDto> keywords;
    private List<DiaryEmotionDto> emotions;
}
