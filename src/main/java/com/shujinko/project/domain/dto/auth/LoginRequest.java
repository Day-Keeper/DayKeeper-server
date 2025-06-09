package com.shujinko.project.domain.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginRequest {
    private String idToken;
    private String birthday;
    private String accessToken;
}