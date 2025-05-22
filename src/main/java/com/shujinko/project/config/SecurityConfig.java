package com.shujinko.project.config;

import com.shujinko.project.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {//검증 성공 시, 인증 정보를 SecurityContext에 등록 → 로그인 상태처럼 동작
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/v3/api-docs/**",
                                "/webjars/**").permitAll()// /auth로 시작하는 주소는 누구나 접근가능
                        .anyRequest().authenticated() // 다른 주소는 반드시 로그인한 사람만 접근가능
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // Token 존재하는지, 유효한지 Spring Security에 저장
                .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)//http basic방식과 Form Login방식 끄기
                .sessionManagement((session)->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))//STATELESS
                .build();
    }
}