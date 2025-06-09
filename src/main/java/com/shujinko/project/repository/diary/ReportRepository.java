package com.shujinko.project.repository.diary;

import com.shujinko.project.domain.entity.diary.Report;
import com.shujinko.project.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByUserAndYearAndMonthAndWeekOfMonthAndSentenceType(User user, int year, int month, int weekOfMonth, String sentenceType);
    Optional<Report> findByUserAndSentenceType(User user, String sentenceType);
    List<Report> findByUserAndYearAndMonthAndSentenceType(User user, int year, int month, String sentenceType);
    List<Report> findByUserAndYearAndMonthAndSentenceTypeOrderByWeekOfMonthAsc(User user, int year, int month, String sentenceType);
}
