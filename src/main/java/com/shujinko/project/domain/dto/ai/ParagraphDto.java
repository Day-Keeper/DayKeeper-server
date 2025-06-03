package com.shujinko.project.domain.dto.ai;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ParagraphDto {
    private String subject;
    private String content;
    private String matched_image;
    private String image_caption;
}
