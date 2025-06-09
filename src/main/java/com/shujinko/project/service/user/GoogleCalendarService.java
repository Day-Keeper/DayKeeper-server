package com.shujinko.project.service.user;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.Json;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.OAuth2Credentials;
import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.provider.GoogleTokenVerifier;
import com.shujinko.project.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.List;
@Service
public class GoogleCalendarService {
    
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);
    
    public GoogleCalendarService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    private Calendar getCalendarService(String accessToken) throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
        
        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .build();
    }
    
    public List<Event> getCalendarEvents(String uid) throws GeneralSecurityException,IOException {
        User user = userRepository.findByUid(uid);
        if(user==null){
            logger.error("user not found");
        }
        String accessToken = user.getAccessToken();
        Calendar service = getCalendarService(accessToken);
        
        Events events = service.events().list("primary")
                .setMaxResults(10)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();
        
        return events.getItems();
    }
}
