package com.shujinko.project.controller.diary;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.calendar.model.Event;
import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.repository.user.UserRepository;
import com.shujinko.project.service.user.AuthService;
import com.shujinko.project.service.user.GoogleCalendarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RequestMapping("/calendar")
@RestController
public class GoogleCalendarController {
    private final GoogleCalendarService googleCalendarService;
    
    public GoogleCalendarController(GoogleCalendarService googleCalendarService, UserRepository userRepository, AuthService authService) {
        this.googleCalendarService = googleCalendarService;
    }
    
    @PostMapping("/load")
    public ResponseEntity<?> calendarCallback(Authentication auth) {
        try {
            String uid = auth.getName();
            CompletableFuture<Map<LocalDate,List<String>>> events = googleCalendarService.getCalendarEvents(uid);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            // 모든 예외는 여기서 처리 (예: 401, 400 등)
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("캘린더 일정을 가져오는데 실패했습니다. 접근 권한을 확인해주세요.");
        }
    }
}
