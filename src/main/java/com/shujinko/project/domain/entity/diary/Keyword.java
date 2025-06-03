package com.shujinko.project.domain.entity.diary;

import com.shujinko.project.domain.dto.ai.aiKeywordDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "keyword", uniqueConstraints = { // @Table 어노테이션 추가
        @UniqueConstraint(columnNames = {"keywordStr", "label"}) // 복합 유니크 제약 조건 설정
})
public class Keyword {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 30, nullable = false)
    private String keywordStr;
    
    @Column(length = 30)
    private String label;
    
    @OneToMany(mappedBy = "keyword", fetch = FetchType.LAZY)
    private List<DiaryKeyword> diaryKeywords = new ArrayList<>();
    
    public aiKeywordDto toAiKeywordDto(){
        return aiKeywordDto.builder()
                .text(keywordStr).label(label).build();
    }
}

