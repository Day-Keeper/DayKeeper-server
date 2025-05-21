package com.shujinko.project.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class RefreshToken {
    @Id
    private String uid;

    private String token;

    private LocalDateTime expiryDate;
}