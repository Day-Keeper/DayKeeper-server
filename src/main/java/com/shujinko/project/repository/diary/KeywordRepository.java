package com.shujinko.project.repository.diary;

import com.shujinko.project.domain.entity.diary.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    Optional<Keyword> findByKeywordStr(String keyword);
    List<Keyword> findByKeywordStrIn(List<String> keywords);
}
