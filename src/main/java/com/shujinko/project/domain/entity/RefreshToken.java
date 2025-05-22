package com.shujinko.project.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
public class RefreshToken {
    @Id
    private String uid;

    private String token;

    private LocalDateTime expiryDate;
}