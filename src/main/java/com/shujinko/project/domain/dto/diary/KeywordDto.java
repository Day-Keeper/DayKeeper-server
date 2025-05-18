package com.shujinko.project.domain.dto.diary;

import com.shujinko.project.domain.entity.diary.Keyword;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class KeywordDto {
    private String keyword;
}
