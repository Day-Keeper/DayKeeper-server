package com.shujinko.project.repository.diary;

import com.shujinko.project.domain.entity.diary.WeeklyKeywordStat;
import com.shujinko.project.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeeklyKeywordStatRepository extends JpaRepository<WeeklyKeywordStat, Long> {
    
    // 특정 사용자의 특정 연도, 특정 주차의 통계를 삭제하기 위한 메서드
    void deleteByUserAndYearAndWeekOfYear(User user, int year, int weekOfYear);
    
    // 특정 사용자의 특정 연도, 특정 주차의 Top N 키워드를 빈도수 내림차순으로 조회
    List<WeeklyKeywordStat> findByUserAndYearAndWeekOfYearOrderByFrequencyDesc(User user, int year, int weekOfYear);
    
    List<WeeklyKeywordStat> findByWeekOfYear(int weekOfYear);
    
    // 페이징을 적용하여 Top N개를 가져오고 싶다면 Pageable 사용 가능
    // Page<WeeklyKeywordStat> findByUserAndYearAndWeekOfYearOrderByFrequencyDesc(User user, int year, int weekOfYear, Pageable pageable);
}
