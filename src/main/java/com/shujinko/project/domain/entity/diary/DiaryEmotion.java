package com.shujinko.project.domain.entity.diary;

import com.shujinko.project.domain.dto.ai.EmotionScoreDto;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class DiaryEmotion {
    @EqualsAndHashCode.Include
    @EmbeddedId
    @Builder.Default
    private DiaryEmotionId id = new DiaryEmotionId();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("did")
    @JoinColumn(name = "did")
    private Diary diary;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("eid")
    @JoinColumn(name = "eid")//이 테이블에 DB에 "DiaryEmotion에 eid"라는 FK컬럼 생성
    private Emotion emotion;
    
    private double score;
    
    public EmotionScoreDto toEmotionScoreDto() {
        return EmotionScoreDto.builder().score(score).
                emotion(emotion.getEmotionStr()).
                build();
    }
    
}
