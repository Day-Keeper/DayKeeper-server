package com.shujinko.project.repository.diary;

import com.shujinko.project.domain.entity.diary.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {
    List<Diary> findByUser_Uid(String userId);
    List<Diary> findByUser_UidAndCreatedAtBetween(String userId, LocalDateTime start, LocalDateTime end);
}
