package com.shujinko.project.domain.entity.diary;

import com.shujinko.project.domain.dto.ai.AiKeywordDto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
public class DiaryKeyword {
    @EqualsAndHashCode.Include
    @EmbeddedId
    @Builder.Default
    private DiaryKeywordId id = new DiaryKeywordId();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("did")
    @JoinColumn(name = "did")
    private Diary diary;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("kid")
    @JoinColumn(name = "kid")
    private Keyword keyword;
    
    public String byString(){
        return keyword.getKeywordStr();
    }
    
    public AiKeywordDto toAiKeywordDto(){
        if(keyword != null) {
            return AiKeywordDto.builder().
                    text(keyword.getKeywordStr()).
                    label(keyword.getLabel()).
                    build();
        }
        return AiKeywordDto.builder().
                text(keyword.getKeywordStr()).
                label(null).
                build();
    }
}
