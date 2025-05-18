package com.shujinko.project.service.user;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.firebase.auth.*;
import com.shujinko.project.config.JwtConfig;
import com.shujinko.project.provider.JwtTokenProvider;
import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.provider.GoogleTokenVerifier;
import com.shujinko.project.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private GoogleTokenVerifier googleTokenVerifier;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public String authenticate(String idToken) throws FirebaseAuthException {
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

        return jwtTokenProvider.createToken(uid, email, name);
    }
}