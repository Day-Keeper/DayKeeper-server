package com.shujinko.project.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginRequest {
    private String idToken;
    private String accessToken;
}