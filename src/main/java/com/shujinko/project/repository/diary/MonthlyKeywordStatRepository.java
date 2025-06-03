package com.shujinko.project.repository.diary;

import com.shujinko.project.domain.entity.diary.MonthlyKeywordStat;
import com.shujinko.project.domain.entity.user.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MonthlyKeywordStatRepository extends JpaRepository<MonthlyKeywordStat, Long> {
    
    @Modifying
    @Query("DELETE FROM MonthlyKeywordStat m WHERE m.user = :user AND m.year = :year AND m.month = :month")
    void deleteByUserAndYearAndMonth(User user, int year,int month);
    
    // 특정 사용자의 특정 연도, 특정 주차의 Top N 키워드를 빈도수 내림차순으로 조회
    List<MonthlyKeywordStat> findByUserAndYearAndMonthOrderByFrequencyDesc(User user, int year, int month);
    
    List<MonthlyKeywordStat> findByUserAndYearAndMonth(User user, int year, int month);
}
