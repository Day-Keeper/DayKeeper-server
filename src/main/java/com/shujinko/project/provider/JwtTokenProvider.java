//JWT 토큰을 생성/검증/파싱하는 유틸 클래스
package com.shujinko.project.provider;

import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.repository.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    
    @Value("${jwt.secret}")
    private String secretKeyString;

    private Key secretKey;
    
    private final RefreshTokenRepository refreshTokenRepository;
    
    public JwtTokenProvider(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }
    
    @PostConstruct
    protected void init() {
        secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String uid, String email, String name) {
    /**
    * Token에 uid/email/name을 저장해서 토큰발급
    * */
        Claims claims = Jwts.claims().setSubject(uid);
        claims.put("email", email);
        claims.put("name", name);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + 3600000);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(String uid) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 1209600000L); // 2주
        
        return Jwts.builder()
                .setSubject(uid)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey)
                .compact();
    }
    
    /**
    *Bearer eawefnbsdbfhwae에서 Bearer가 있다면 Bearer지우고 payload만 꺼내오기 || return NULL
    * */
    public String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {//Bearer eawefnbsdbfhwae에서 Bearer가 있다면 Bearer지우고 payload만 꺼내오기
            return bearer.substring(7);
        }
        return null;
    }

    /**
    * jwt 파서생성(토큰이 올바르게 서명(secretKey로)됐는지/만료안됐는지/형식이 유효한지 체크
    * */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);//secretKey로 파서 만들고(build), token을 파시아하고 유효성검증함
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("JWT 검증 실패 : {}", e.getMessage());
            return false;
        }
    }

    public UsernamePasswordAuthenticationToken getAuthentication(String token) {
        String uid = getUid(token);
        return new UsernamePasswordAuthenticationToken(uid, "", List.of()); //principle(sub), credentials(password), 권한목록
    }

    
    /**
    * Token에서 sub(uid)만 빼오기
    * */
    public String getUid(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token)
                .getBody().getSubject();
    }
}
