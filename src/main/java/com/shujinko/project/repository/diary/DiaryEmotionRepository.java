package com.shujinko.project.repository.diary;

import com.shujinko.project.domain.entity.diary.Diary;
import com.shujinko.project.domain.entity.diary.DiaryEmotion;
import com.shujinko.project.domain.entity.diary.DiaryEmotionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiaryEmotionRepository extends JpaRepository<DiaryEmotion, DiaryEmotionId> {
    public Optional<DiaryEmotion> findByDiary(Diary Diary);
}