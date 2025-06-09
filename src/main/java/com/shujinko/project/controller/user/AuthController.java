package com.shujinko.project.controller.user;

import com.google.firebase.auth.FirebaseAuthException;
import com.shujinko.project.domain.dto.auth.LoginRequest;
import com.shujinko.project.domain.dto.auth.RefreshRequest;
import com.shujinko.project.service.user.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final Logger logger = LoggerFactory.getLogger(AuthController.class);
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) throws GeneralSecurityException, IOException {//Google id token 받음
        logger.info("idToken = {}", request.getIdToken());
        if (request.getIdToken() == null || request.getIdToken().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("idToken 누락");
        }
        logger.info("accessToken = {}", request.getAccessToken());
        try {
            // Firebase 인증을 통해 Google ID 토큰 검증 및 사용자 정보 획득
            var tokens = authService.authenticate(request);
            logger.info("AccessToken 발급 성공: {}", tokens.getAccessToken());
            logger.info("RefreshToken 발급 성공: {}", tokens.getRefreshToken());
            return ResponseEntity.ok(tokens);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid ID token");
        }
    }
    
//    @PostMapping("/testLogin")
//    public ResponseEntity<?> testLogin(@RequestBody LoginRequestTest request) {
//
//    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            if (refreshToken == null || refreshToken.isBlank()) {
                return ResponseEntity.badRequest().body("refreshToken 누락");
            }

            String newAccessToken = authService.refreshJwtAccessToken(refreshToken);
            return ResponseEntity.ok(Collections.singletonMap("accessToken", newAccessToken));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh Token 유효하지 않음");
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> catchAll(Exception e) {
        System.err.println("서버 전체 예외 처리: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(400).body("예외 발생: " + e.getMessage());
    }
}