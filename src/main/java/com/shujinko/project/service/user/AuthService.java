package com.shujinko.project.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.firebase.auth.*;
import com.shujinko.project.config.JwtConfig;
import com.shujinko.project.domain.dto.LoginResponse;
import com.shujinko.project.domain.entity.RefreshToken;
import com.shujinko.project.provider.JwtTokenProvider;
import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.provider.GoogleTokenVerifier;
import com.shujinko.project.repository.RefreshTokenRepository;
import com.shujinko.project.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private GoogleTokenVerifier googleTokenVerifier;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public LoginResponse authenticate(String idToken) throws FirebaseAuthException {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(idToken);
        if (payload == null) {
            throw new RuntimeException("유효하지 않은 구글 토큰입니다.");
        }

        String uid = payload.getSubject(); // 고유 사용자 ID (uid 대신)
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String photoUrl = (String) payload.get("picture");

        System.out.println("사용자 정보: " + uid + ", " + email + ", " + name + ", " + photoUrl);


        // 로그인한 사용자가 DB에 없으면 자동 회원가입
        User user = userRepository.findById(uid).orElseGet(() ->
                userRepository.save(
                        User.builder()
                                .uid(uid)
                                .email(email)
                                .name(name)
                                .photoUrl(photoUrl)
                                .createdAt(LocalDateTime.now())
                                .build()
                )
        );

        String accessToken = jwtTokenProvider.createAccessToken(uid, email, name);
        String refreshToken = jwtTokenProvider.createRefreshToken(uid);

        refreshTokenRepository.save(
                new RefreshToken(uid, refreshToken, LocalDateTime.now().plusDays(14))
        );

        return new LoginResponse(accessToken, refreshToken);
    }

    public String refreshAccessToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 refresh token"));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("refresh token 만료됨");
        }

        User user = userRepository.findById(token.getUid())
                .orElseThrow(() -> new RuntimeException("사용자 정보 없음"));

        return jwtTokenProvider.createAccessToken(user.getUid(), user.getEmail(), user.getName());
    }

}