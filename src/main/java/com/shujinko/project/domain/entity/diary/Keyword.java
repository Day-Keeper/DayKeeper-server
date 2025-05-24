package com.shujinko.project.domain.entity.diary;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class Keyword {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, length = 30, nullable = false)
    private String keywordStr;
    
    @OneToMany(mappedBy = "keyword", fetch = FetchType.LAZY)
    private List<DiaryKeyword> diaryKeywords = new ArrayList<>();
}

