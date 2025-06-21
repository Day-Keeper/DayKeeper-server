package com.shujinko.project.domain.dto.ai;


import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionReqToGemini {
    String raw_diary;
    String schedules;
    int age;
}
