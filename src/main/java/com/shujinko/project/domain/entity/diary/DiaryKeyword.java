package com.shujinko.project.domain.entity.diary;

import com.shujinko.project.domain.dto.ai.aiKeywordDto;
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
    
    public aiKeywordDto toAiKeywordDto(){
        if(keyword != null) {
            return aiKeywordDto.builder().
                    text(keyword.getKeywordStr()).
                    label(keyword.getLabel()).
                    build();
        }
        return aiKeywordDto.builder().
                text(keyword.getKeywordStr()).
                label(null).
                build();
    }
}
