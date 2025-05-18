package com.shujinko.project.domain.entity.diary;

import com.shujinko.project.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Diary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "uid")
    private User user;
    
    @Lob
    private String rawDiary;
    
    @Lob
    private String rephrasedDiary;
    
    private LocalDateTime createdAt;
    
    @Lob
    private String summary;
    
    @ManyToMany
    @JoinTable(
            name = "diary_keyword",
            joinColumns = @JoinColumn(name = "did"),
            inverseJoinColumns = @JoinColumn(name = "id")
    )
    private List<Keyword> keywords = new ArrayList<>();
    
    @OneToMany(mappedBy = "diary")
    private List<DiaryEmotion> diaryEmotions = new ArrayList<>();
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Diary diary = (Diary) o;
        return id != null && id.equals(diary.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
