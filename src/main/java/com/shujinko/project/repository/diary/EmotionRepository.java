package com.shujinko.project.repository.diary;

import com.shujinko.project.domain.entity.diary.Emotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmotionRepository extends JpaRepository<Emotion, Long> {
    public Emotion findByEmotionStr(String emotion);
    public List<Emotion> findByEmotionStrIn(List<String> keywords);
}
