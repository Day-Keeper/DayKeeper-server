package com.shujinko.project.domain.entity.diary;

import com.shujinko.project.domain.dto.diary.DiaryResponseDto;
import com.shujinko.project.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class Diary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid")
    private User user;
    
    @Lob
    private String rawDiary;
    
    @Lob
    private String rephrasedDiary;
    
    private LocalDateTime createdAt;
    
    @Lob
    private String summary;
    
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DiaryKeyword> diaryKeywords = new ArrayList<>();
    
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DiaryEmotion> diaryEmotions = new ArrayList<>();
    
    private String LabelEmotion = "";
    
    
    public DiaryResponseDto toResponseDto(){
        return DiaryResponseDto.builder().
                rawDiary(this.rawDiary).
                rephrasedDiary(this.rephrasedDiary).
                createdAt(this.createdAt).
                summary(this.summary).
                label(this.LabelEmotion).
                keywords(this.diaryKeywords.stream().map(DiaryKeyword::toAiKeywordDto).toList()).
                emotions(this.diaryEmotions.stream().map(DiaryEmotion::toEmotionScoreDto).toList()).
                diaryId(this.id).build();
        
    }
}

