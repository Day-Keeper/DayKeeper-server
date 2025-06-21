package com.shujinko.project.domain.dto.ai;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class AiKeywordDto {
    private String text;
    private String label;
}
