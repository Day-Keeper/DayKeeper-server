package com.shujinko.project.provider;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.shujinko.project.domain.dto.auth.LoginResponse;
import com.shujinko.project.domain.entity.RefreshToken;
import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.repository.RefreshTokenRepository;
import com.shujinko.project.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;

@Component
public class GoogleTokenVerifier {

    @Value("${app.auth.google.trusted-audiences}")
    private List<String> trustedAudiences;
    
    private static final HttpTransport transport = Utils.getDefaultTransport();
    private static final JsonFactory jsonFactory = Utils.getDefaultJsonFactory();

    
    public GoogleTokenVerifier(UserService userService,
                               JwtTokenProvider jwtTokenProvider,
                               RefreshTokenRepository refreshTokenRepository) {
    }
    
    public GoogleIdToken.Payload verify(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                    .setAudience(trustedAudiences)//aud:xxx라는 항목이 trustedAudiences에 있는지 확인
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            return idToken != null ? idToken.getPayload() : null;
        } catch (Exception e) {
            return null;
        }
    }
}