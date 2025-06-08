package com.shujinko.project.repository.diary;

import com.shujinko.project.domain.entity.diary.Day30KeywordStat;
import com.shujinko.project.domain.entity.diary.Day7KeywordStat;
import com.shujinko.project.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface Day30KeywordStatRepository extends JpaRepository<Day30KeywordStat, Long> {
    List<Day30KeywordStat> findByUser(User user);
    
    @Modifying
    @Query("DELETE FROM Day30KeywordStat s WHERE s.user = :user")
    void deleteByUser(User user);
}
