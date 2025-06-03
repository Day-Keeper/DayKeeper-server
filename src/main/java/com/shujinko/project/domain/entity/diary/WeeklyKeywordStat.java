package com.shujinko.project.domain.entity.diary;

import com.shujinko.project.domain.dto.diary.KeywordCountDto;
import com.shujinko.project.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "weekly_keyword_stat", indexes = { // 검색 성능을 위한 인덱스 추가
        @Index(name = "idx_weekly_keyword_stat_user_year_week", columnList = "uid, year, weekOfYear")
})
public class WeeklyKeywordStat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid", nullable = false) // User 엔티티의 PK 컬럼명에 맞게 수정 필요 (예: uid 또는 id)
    private User user;
    
    @Column(nullable = false)
    private int year;
    
    @Column(nullable = false)
    private int weekOfYear; // ISO 8601 주차
    
    @Column(length = 30, nullable = false) // Keyword의 keywordStr과 길이 일치 또는 적절히 조절
    private String keywordStr;
    
    @Column(length=30)
    private String label;
    
    @Column(nullable = false)
    private Long frequency;
    
    // 필요하다면 순위(rank) 필드도 추가 가능
    // private int rank;
    
    public KeywordCountDto toKeywordCountDto() {
        return KeywordCountDto.builder().
                keyword(keywordStr).label(label).count(frequency).build();
    }
}
