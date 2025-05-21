package com.shujinko.project.domain.dto.diary;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiaryRequestDto {
    private int year;
    private int month;
    private int day;
}
