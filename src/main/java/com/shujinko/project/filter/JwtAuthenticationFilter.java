package com.shujinko.project.filter;

import com.shujinko.project.provider.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        logger.info("필터 진입: {}",path);

        // /auth는 무조건 통과
        if (path.startsWith("/auth")) {
            logger.info("필터 예외 처리: " +path);
            filterChain.doFilter(request, response);//다음 필터 OR 컨트롤러로 요청을 넘김
            return;
        }

        String token = jwtTokenProvider.resolveToken(request);

        // 토큰이 아예 없으면 통과
        if (token == null) {
            System.out.println("    토큰 없음, 필터 통과");
            filterChain.doFilter(request, response);
            return;
        }

        // 토큰 있을 경우 검증
        if (jwtTokenProvider.validateToken(token)) {//토큰이 올바른지 검증
            Authentication auth = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);//
        }

        filterChain.doFilter(request, response);
    }

}
