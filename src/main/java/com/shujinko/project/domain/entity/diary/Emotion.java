package com.shujinko.project.domain.entity.diary;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Emotion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 20)
    private String emotion;
    
    @OneToMany(mappedBy = "emotion")
    private List<DiaryEmotion> diaryEmotions = new ArrayList<>();
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Emotion emotion = (Emotion) o;
        return id != null && id.equals(emotion.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

