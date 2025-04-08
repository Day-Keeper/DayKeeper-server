package com.shujinko.project.domain.entity;



import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Image")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long image_Id;
    
    @Column(nullable = false)
    String url;
    
    @Column(nullable = true)
    LocalDateTime takentime;
}
