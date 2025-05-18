package com.shujinko.project.domain.dto.user;


import com.shujinko.project.domain.entity.user.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserDto {
    private String uid;
    private String email;
    private String name;
    private String photoUrl;
    private LocalDateTime createdAt;
    
    public User toEntity(){
        return User.builder()
                .uid(uid)
                .email(email)
                .name(name)
                .photoUrl(photoUrl)
                .createdAt(createdAt)
                .build();
    }
}
