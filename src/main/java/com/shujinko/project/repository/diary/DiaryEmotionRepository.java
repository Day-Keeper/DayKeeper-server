package com.shujinko.project.repository.diary;

import com.shujinko.project.domain.entity.diary.Diary;
import com.shujinko.project.domain.entity.diary.DiaryEmotion;
import com.shujinko.project.domain.entity.diary.DiaryEmotionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryEmotionRepository extends JpaRepository<DiaryEmotion, DiaryEmotionId> {
    public DiaryEmotion findByDiary(Diary Diary);
}
