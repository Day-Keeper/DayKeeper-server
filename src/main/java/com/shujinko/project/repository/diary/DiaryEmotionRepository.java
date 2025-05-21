package com.shujinko.project.repository.diary;

import com.shujinko.project.domain.entity.diary.Diary;
import com.shujinko.project.domain.entity.diary.DiaryEmotion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryEmotionRepository extends JpaRepository<DiaryEmotion, Integer> {
    public DiaryEmotion findByDiary(Diary Diary);
}
