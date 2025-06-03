package com.shujinko.project.domain.dto.ai;


import lombok.*;

import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class aiResponseDto {
    private List<ParagraphDto> paragraphs;
    private String summary;
    private aiEmotionDto emotion;
    private List<aiKeywordDto> keywords;
}
