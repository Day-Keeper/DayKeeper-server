package com.shujinko.project.domain.entity.diary;

import com.shujinko.project.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sentenceId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid")
    private User user;
    
    private int year;
    
    private int month;
    
    private int weekOfMonth;
    
    private String sentenceType;
    
    private String sentence;
}
