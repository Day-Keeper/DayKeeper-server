package com.shujinko.project.repository.diary;

import com.shujinko.project.domain.entity.diary.DiaryKeyword;
import com.shujinko.project.domain.entity.diary.DiaryKeywordId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryKeywordRepository extends JpaRepository<DiaryKeyword, DiaryKeywordId> {
}
