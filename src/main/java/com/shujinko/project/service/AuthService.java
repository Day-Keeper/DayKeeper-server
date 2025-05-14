package com.shujinko.project.service;

import com.google.firebase.auth.*;
import com.shujinko.project.domain.entity.User;
import com.shujinko.project.repository.UserRepository;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    public String authenticate(String idToken) throws FirebaseAuthException {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        String uid = decodedToken.getUid();
        String email = decodedToken.getEmail();
        String name = (String) decodedToken.getClaims().get("name");
        String photoUrl = (String) decodedToken.getClaims().get("picture");

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

        // JWT 생성 로직 (예: jjwt 라이브러리 사용)
        String jwt = Jwts.builder()
                .setSubject(uid)
                .claim("email", email)
                .claim("name", name)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)))
                .signWith(SignatureAlgorithm.HS256, "your-secret-key")
                .compact();

        return jwt;
    }
}