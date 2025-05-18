package com.shujinko.project.domain.entity.diary;

import jakarta.persistence.*;
import lombok.*;

import java.security.Key;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Keyword {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 30)
    private String keyword;
    
    @ManyToMany(mappedBy = "keywords")
    private List<Diary> diaries = new ArrayList<>();
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Keyword keyword = (Keyword) o;
        return id != null && id.equals(keyword.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

