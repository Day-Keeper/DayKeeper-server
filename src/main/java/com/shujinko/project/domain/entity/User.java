package com.shujinko.project.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String uid;
    @Column(unique = true)
    private String email;
    @Column(length=20)
    private String name;
    private String photoUrl;
    private LocalDateTime createdAt;
}