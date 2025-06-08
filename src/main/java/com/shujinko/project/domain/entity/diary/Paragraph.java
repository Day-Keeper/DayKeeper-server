package com.shujinko.project.domain.entity.diary;


import com.shujinko.project.domain.dto.ai.ParagraphDto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "paragraph")
public class Paragraph {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "paragraph_id")
    Long paragraphId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    Diary diary;
    
    String subject;
    
    @Lob
    String content;
    
    String photoURL;
    
    public ParagraphDto toDto(){
        return ParagraphDto.builder().subject(subject).content(content).matched_image(photoURL).build();
    }
}
