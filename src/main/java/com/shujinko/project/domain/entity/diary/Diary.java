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
    
    @Builder.Default
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private List<Paragraph> paragraphs = new ArrayList<>();
    
    private LocalDateTime createdAt;
    
    @Lob
    private String summary;
    @Builder.Default
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DiaryKeyword> diaryKeywords = new ArrayList<>();
    @Builder.Default
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DiaryEmotion> diaryEmotions = new ArrayList<>();
    @Builder.Default
    private String LabelEmotion = "";
    @Builder.Default
    @OneToMany(mappedBy = "diary" , cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UnmatchedImage> unmatchedImages = new ArrayList<>();
    
    
    public DiaryResponseDto toResponseDto(){
        return DiaryResponseDto.builder().
                rawDiary(this.rawDiary).
                paragraph(paragraphs.stream().map(Paragraph::toDto).toList()).
                createdAt(this.createdAt).
                summary(this.summary).
                label(this.LabelEmotion).
                keywords(this.diaryKeywords.stream().map(DiaryKeyword::toAiKeywordDto).toList()).
                emotions(this.diaryEmotions.stream().map(DiaryEmotion::toEmotionScoreDto).toList()).
                unmatchedImages(this.unmatchedImages.stream().map(UnmatchedImage::getImageUrl).toList()).
                diaryId(this.id).build();
        
    }
}

