package com.shujinko.project.domain.dto.ai;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SuggestionRequest {
    String rawDiary;
    String diaryDate;
}
