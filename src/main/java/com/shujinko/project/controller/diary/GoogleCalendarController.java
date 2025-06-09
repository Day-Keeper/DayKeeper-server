package com.shujinko.project.controller.diary;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.calendar.model.Event;
import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.repository.user.UserRepository;
import com.shujinko.project.service.user.AuthService;
import com.shujinko.project.service.user.GoogleCalendarService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@RequestMapping("/calendar")
@Controller
public class GoogleCalendarController {
    private final GoogleCalendarService googleCalendarService;
    private final UserRepository userRepository;
    private final AuthService authService;
    
    public GoogleCalendarController(GoogleCalendarService googleCalendarService, UserRepository userRepository, AuthService authService) {
        this.googleCalendarService = googleCalendarService;
        this.userRepository = userRepository;
        this.authService = authService;
    }
    
    @PostMapping("/test")
    public ResponseEntity<List<Event>> calendarCallback(Authentication auth) throws IOException, GeneralSecurityException {
        String uid = auth.getName();
        try
        {
            List<Event> events = googleCalendarService.getCalendarEvents(uid);
            return ResponseEntity.ok(events);
        }
        catch(GoogleJsonResponseException e){
            if(e.getStatusCode() == 401)
                try
                {
                    authService.refreshGoogleAccessToken(uid);
                    List<Event> events = googleCalendarService.getCalendarEvents(uid);
                    return ResponseEntity.ok(events);
                }catch(Exception refreshException){
                    refreshException.printStackTrace();
                    return ResponseEntity.status(401).body(null);
                }
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.badRequest().build();
    }
}
