package com.shujinko.project.domain.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RefreshRequest {
    private String refreshToken;
}