package com.shujinko.project.domain.dto.diary;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class DiaryRequestDto {
    private int year;
    private int month;
    private int day;
}
