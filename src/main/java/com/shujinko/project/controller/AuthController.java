package com.shujinko.project.controller;

import com.google.firebase.auth.FirebaseAuthException;
import com.shujinko.project.domain.dto.LoginRequest;
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
            String jwt = authService.authenticate(request.getIdToken());
            System.out.println("JWT 발급 성공: " + jwt);
            return ResponseEntity.ok(Collections.singletonMap("token", jwt))    ;
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid ID token");
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> catchAll(Exception e) {
        System.err.println("서버 전체 예외 처리: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(400).body("예외 발생: " + e.getMessage());
    }
}