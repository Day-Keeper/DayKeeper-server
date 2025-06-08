package com.shujinko.project.domain.entity.diary;

import com.shujinko.project.domain.dto.diary.KeywordCountDto;
import com.shujinko.project.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class Day30KeywordStat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid",nullable = false)
    User user;
    
    @Column(length = 255)
    private String keywordStr;
    
    @Column(length = 255)
    private String label;
    
    private Long frequency;
    
    public KeywordCountDto toKeywordCountDto() {
        return KeywordCountDto.builder().
                keyword(this.keywordStr).
                label(this.label).
                count(this.frequency).
                build();
    }
}
