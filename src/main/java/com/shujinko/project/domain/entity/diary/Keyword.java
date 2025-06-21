package com.shujinko.project.domain.entity.diary;

import com.shujinko.project.domain.dto.ai.AiKeywordDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"keywordStr","label"}) // keywordStr을 기준으로 equals와 hashCode를 생성
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
    @Builder.Default
    private List<DiaryKeyword> diaryKeywords = new ArrayList<>();
    
    public AiKeywordDto toAiKeywordDto(){
        return AiKeywordDto.builder()
                .text(keywordStr).label(label).build();
    }
}

