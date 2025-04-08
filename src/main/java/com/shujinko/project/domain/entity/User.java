package com.shujinko.project.domain.entity;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long user_id;
    @Column
    private String google;
    @Column
    private String email;
    @Column(length=20)
    private String username;
}
