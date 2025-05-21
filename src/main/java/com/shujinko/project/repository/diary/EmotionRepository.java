package com.shujinko.project.repository.diary;

import com.shujinko.project.domain.entity.diary.Emotion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmotionRepository extends JpaRepository<Emotion, Long> {
    public Emotion findByEmotion(String emotion);
}
