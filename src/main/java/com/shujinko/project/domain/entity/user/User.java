package com.shujinko.project.domain.entity.user;

import com.shujinko.project.domain.entity.diary.Diary;
import com.shujinko.project.domain.entity.diary.MonthlyKeywordStat;
import com.shujinko.project.domain.entity.diary.WeeklyKeywordStat;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {
    @Id
    @Column(name="uid",nullable=false,unique=true)
    private String uid;
    @Column(unique = true)
    private String email;
    @Column(length=20)
    private String name;
    private String photoUrl;
    private LocalDateTime createdAt;
    @OneToMany(mappedBy = "user")
    private List<Diary> diaries = new ArrayList<>();
}