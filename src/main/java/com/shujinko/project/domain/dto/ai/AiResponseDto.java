package com.shujinko.project.domain.dto.ai;


import lombok.*;

import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class AiResponseDto {
    private List<ParagraphDto> paragraphs;
    private String summary;
    private AiEmotionDto emotion;
    private List<AiKeywordDto> keywords;
    private List<String> unmatched_images;
}
