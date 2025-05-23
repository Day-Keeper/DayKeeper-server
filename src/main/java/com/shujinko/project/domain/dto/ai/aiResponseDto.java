package com.shujinko.project.domain.dto.ai;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class aiResponseDto {
    private List<ParagraphDto> paragraphs;
    private aiEmotionDto emotion;
    private List<aiKeywordDto> keywords;
}
