package com.shujinko.project.domain.entity.diary;


import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UnmatchedImage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long unmatchedImageId;
    
    @ManyToOne
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;
    
    private String imageUrl;
}
