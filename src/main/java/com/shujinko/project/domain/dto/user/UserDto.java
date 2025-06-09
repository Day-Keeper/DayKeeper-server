package com.shujinko.project.domain.dto.user;


import com.shujinko.project.domain.entity.user.User;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserDto {
    //private String uid;
    private String email;
    private String name;
    //private String photoUrl;
    //private LocalDateTime createdAt;
    private LocalDate birthday;
}

