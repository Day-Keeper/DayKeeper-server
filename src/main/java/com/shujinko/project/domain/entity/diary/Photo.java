package com.shujinko.project.domain.entity.diary;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "photo")
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id")
    private Long id;
    
    @Column(nullable = false)
    private String url;
    private String mimeType;
    private int orderIndex; //사진에서의 순서
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diary_id",nullable = false)
    private Diary diary;
}
