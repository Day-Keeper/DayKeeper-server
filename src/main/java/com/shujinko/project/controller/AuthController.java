package com.shujinko.project.controller;

import com.google.firebase.auth.FirebaseAuthException;
import com.shujinko.project.domain.dto.LoginRequest;
import com.shujinko.project.domain.dto.RefreshRequest;
import com.shujinko.project.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        System.out.println("idToken = " + request.getIdToken());
        if (request.getIdToken() == null || request.getIdToken().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("idToken 누락");
        }

        try {
            var tokens = authService.authenticate(request.getIdToken());

            System.out.println("AccessToken 발급 성공: " + tokens.getAccessToken());
            System.out.println("RefreshToken 발급 성공: " + tokens.getRefreshToken());

            return ResponseEntity.ok(tokens);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid ID token");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            if (refreshToken == null || refreshToken.isBlank()) {
                return ResponseEntity.badRequest().body("refreshToken 누락");
            }

            String newAccessToken = authService.refreshAccessToken(refreshToken);
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