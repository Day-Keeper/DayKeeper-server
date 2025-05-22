package com.shujinko.project.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RefreshRequest {
    private String refreshToken;
}