package com.shujinko.project.domain.dto;


import com.shujinko.project.domain.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {
    private Long user_id;
    private String google;
    private String email;
    private String username;
    
    public User toEntity(){
        return User.builder()
                .google(google)
                .email(email)
                .username(username)
                .build();
    }
}
