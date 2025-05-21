package com.shujinko.project.domain.entity.diary;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class DiaryEmotion {
    
    @EmbeddedId
    private DiaryEmotionId id = new DiaryEmotionId();
    
    @ManyToOne
    @MapsId("did")
    @JoinColumn(name = "did")
    private Diary diary;
    
    @ManyToOne
    @MapsId("eid")
    @JoinColumn(name = "eid")//이 테이블에 DB에 "DiaryEmotion에 eid"라는 FK컬럼 생성
    private Emotion emotion;
    
    private float score;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiaryEmotion diaryEmotion = (DiaryEmotion) o;
        return id != null && id.equals(diaryEmotion.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
