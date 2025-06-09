package com.shujinko.project.service.user;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.firebase.auth.*;
import com.shujinko.project.config.JwtConfig;
import com.shujinko.project.domain.dto.auth.LoginRequest;
import com.shujinko.project.domain.dto.auth.LoginResponse;
import com.shujinko.project.domain.entity.RefreshToken;
import com.shujinko.project.provider.JwtTokenProvider;
import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.provider.GoogleTokenVerifier;
import com.shujinko.project.repository.RefreshTokenRepository;
import com.shujinko.project.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AuthService {
    
    private final Logger logger = LoggerFactory.getLogger(AuthService.class);
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
    @Autowired
    private GooglePeopleService googlePeopleService;
    @Value("${app.auth.google.web.test-id}")
    private String web_client_id;
    @Value("${app.auth.google.web.test-secret}")
    private String web_client_secret;
    @Value("${app.auth.google.web.redirect-uri}")
    private String redirect_uri;
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    
    

    public LoginResponse authenticate(LoginRequest request) throws FirebaseAuthException, GeneralSecurityException, IOException {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.getIdToken());//구글이 발급한 키가 맞냐(인증)
        if (payload == null) {
            throw new RuntimeException("유효하지 않은 구글 토큰입니다.");
        }

        String uid = payload.getSubject(); // 고유 사용자 ID (uid 대신)
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String photoUrl = (String) payload.get("picture");
        LocalDate birthday = LocalDate.parse(request.getBirthday());

        System.out.println("사용자 정보: " + uid + ", " + email + ", " + name + ", " + photoUrl);
        logger.info("\n--- 사용자 정보 ---\n");
        
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        
        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                HTTP_TRANSPORT,
                JSON_FACTORY,
                web_client_id,
                web_client_secret,
                request.getAccessToken(), //AuthCode
                redirect_uri).execute();
        
        String idToken = tokenResponse.getIdToken();
        String refreshToken = tokenResponse.getRefreshToken();
        String accessToken = tokenResponse.getAccessToken();
        logger.info("ID Token: {}", idToken);
        logger.info("Refresh Token: {}", refreshToken);
        logger.info("Access Token: {}", accessToken);
        // 로그인한 사용자가 DB에 없으면 자동 회원가입
        LocalDate finalBirthday = birthday;
        User user = userRepository.findById(uid).orElseGet(() ->
                userRepository.save(
                        User.builder()
                                .uid(uid)
                                .email(email)
                                .name(name)
                                .photoUrl(photoUrl)
                                .birthday(finalBirthday)
                                .refreshToken(refreshToken)
                                .accessToken(accessToken)
                                .createdAt(LocalDateTime.now())
                                .build()
                )
        );

        String jwtAccessToken = jwtTokenProvider.createAccessToken(uid, email, name);
        String jwtRefreshToken = jwtTokenProvider.createRefreshToken(uid);

        refreshTokenRepository.save(
                new RefreshToken(uid, refreshToken, LocalDateTime.now().plusDays(14))
        );

        return new LoginResponse(jwtAccessToken, jwtRefreshToken);
    }
    

    public String refreshJwtAccessToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 refresh token"));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("refresh token 만료됨");
        }

        User user = userRepository.findById(token.getUid())
                .orElseThrow(() -> new RuntimeException("사용자 정보 없음"));

        return jwtTokenProvider.createAccessToken(user.getUid(), user.getEmail(), user.getName());
    }
    
    @Transactional
    public void refreshGoogleAccessToken(String uid) throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        User user = userRepository.findByUid(uid);
        if (user == null) {
            logger.error("User not found with uid: {}", uid);
            throw new RuntimeException("사용자 정보 없음");
        }
        String refreshToken = user.getRefreshToken();
        
        GoogleTokenResponse response = new GoogleRefreshTokenRequest(
                HTTP_TRANSPORT,
                JSON_FACTORY,
                refreshToken,
                web_client_id,
                web_client_secret)
                .execute();
        
        String newAccessToken = response.getAccessToken();
        
        user.setAccessToken(newAccessToken);
        logger.info("Refresh Access Token: {}", newAccessToken);
        
        return newAccessToken;
    }
    

}