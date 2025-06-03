package com.shujinko.project.repository.diary;

import com.shujinko.project.domain.entity.diary.WeeklyKeywordStat;
import com.shujinko.project.domain.entity.user.User;
import jakarta.transaction.Transactional;
import org.springframework.cglib.core.Local;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WeeklyKeywordStatRepository extends JpaRepository<WeeklyKeywordStat, Long> {
    
    // 특정 사용자의 특정 연도, 특정 주차의 통계를 삭제하기 위한 메서드
    @Modifying
    @Query("DELETE FROM WeeklyKeywordStat w WHERE w.user = :user AND w.weekOfYear = :weekOfYear")
    void deleteByUserAndWeekOfYear(@Param("user") User user, @Param("weekOfYear") LocalDate weekOfYear);
    
    // 특정 사용자의 특정 연도, 특정 주차의 Top N 키워드를 빈도수 내림차순으로 조회
    List<WeeklyKeywordStat> findByUserAndWeekOfYearOrderByFrequencyDesc(User user, LocalDate weekOfYear);
    
    List<WeeklyKeywordStat> findByUserAndWeekOfYear(User user,LocalDate weekOfYear);
}
