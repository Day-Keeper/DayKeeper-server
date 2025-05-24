package com.shujinko.project.domain.dto.diary;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeywordCountDto {
    private String emotion;
    private Long count;
}
