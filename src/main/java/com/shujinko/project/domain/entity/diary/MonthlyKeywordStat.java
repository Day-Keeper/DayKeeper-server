package com.shujinko.project.domain.entity.diary;

import com.shujinko.project.domain.dto.diary.KeywordCountDto;
import com.shujinko.project.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "monthly_keyword_stat", indexes = {
        // 월별 통계에 맞게 인덱스 변경: user, year, month 기준으로 조회 및 정렬
        @Index(name = "idx_monthly_keyword_stat_user_year_month", columnList = "uid, year, month")
})
public class MonthlyKeywordStat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid", nullable = false) // User 엔티티의 PK 컬럼명에 맞게 수정 필요 (예: uid 또는 id)
    private User user;
    
    @Column(nullable = false)
    private int year;
    
    @Column(nullable = false)
    private int month;
    
    @Column(length = 30, nullable = false)
    private String keywordStr;
    
    @Column(length=30)
    private String label; // 레이블 필드
    
    @Column(nullable = false)
    private Long frequency; // 빈도수 필드
    
    // KeywordCountDto 변환 메서드 (기존과 동일하게 유지)
    public KeywordCountDto toKeywordCountDto() {
        return KeywordCountDto.builder()
                .keyword(keywordStr)
                .label(label)
                .count(frequency)
                .build();
    }
}