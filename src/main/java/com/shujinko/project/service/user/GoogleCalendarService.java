package com.shujinko.project.service.user;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GoogleCalendarService {
    
    @Value("${app.auth.google.web.client-id}")
    private String WEB_CLIENT_ID;
    @Value("${app.auth.google.web.client-secret}")
    private String WEB_CLIENT_SECRET;
    private static final NetHttpTransport HTTP_TRANSPORT;
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);
    
    private final Map<String, Calendar> userCalendarServiceCache = new ConcurrentHashMap<>();
    private final Map<String, UserCredentials> userCredentialsCache = new ConcurrentHashMap<>();
    private final Map<String, Map<LocalDate, List<String>>> userEventCache = new ConcurrentHashMap<>();
    
    public GoogleCalendarService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("HTTP Transport 초기화 실패", e);
        }
    }
    
    /**
     * 특정 사용자의 Google Calendar 서비스 클라이언트 객체를 반환
     */
    @Async
    public Calendar getCalendarServiceForUser(String uid) throws IOException, GeneralSecurityException {
        User user = userRepository.findByUid(uid);
        if (user == null || user.getRefreshToken() == null) {
            logger.error("사용자(uid: {})가 존재하지 않거나 리프레시 토큰이 없습니다.", uid);
            throw new RuntimeException("사용자 정보 또는 구글 연동 정보가 없습니다.");
        }
        
        //UserCredentials 객체를 캐시에서 가져오거나 새로 생성합니다.
        UserCredentials credentials = userCredentialsCache.computeIfAbsent(uid, k -> {
            logger.info("사용자 {}를 위한 새로운 UserCredentials 객체 생성.", uid);
            return UserCredentials.newBuilder()
                    .setClientId(WEB_CLIENT_ID)
                    .setClientSecret(WEB_CLIENT_SECRET)
                    .setRefreshToken(user.getRefreshToken())
                    .build();
        });
        
        // 3. Calendar 서비스 객체를 캐시에서 가져오거나 새로 생성합니다.
        Calendar service = userCalendarServiceCache.computeIfAbsent(uid, k -> {
            try {
                logger.info("사용자 {}를 위한 새로운 Calendar 서비스 객체 생성.", uid);
                // GoogleCredentials를 Credential 인터페이스에 맞게 HttpCredentialsAdapter로 감싸서 사용
                HttpCredentialsAdapter adaptedCredentials = new HttpCredentialsAdapter(credentials);
                return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, adaptedCredentials)
                        .setApplicationName("Shujinko Project") // 애플리케이션 이름 설정
                        .build();
            } catch (Exception e) {
                logger.error("사용자 {}의 Calendar 서비스 객체 생성 중 오류 발생: {}", uid, e.getMessage(), e);
                throw new RuntimeException("Calendar 서비스 초기화 실패", e);
            }
        });
        
        return service;
    }
    
    /**
     * 특정 사용자의 Google Calendar 이벤트를 조회하여 날짜별로 그룹화된 Map 형태로 반환
     */
    @Async
    public CompletableFuture<Map<LocalDate, List<String>>> getCalendarEvents(String uid) throws GeneralSecurityException, IOException {
        //캘린더 이벤트 데이터 캐시에서 조회
        if (userEventCache.containsKey(uid)) {
            logger.info("캐시에서 사용자 {}의 캘린더 이벤트 불러옴.", uid);
            return CompletableFuture.completedFuture(userEventCache.get(uid));
        }
        
        //Calendar 서비스 객체 가져오기 (캐시 또는 새로 생성, UserCredentials 활용)
        // 이 부분에서 getCalendarServiceForUser 메서드를 호출하여 UserCredentials 기반으로 Calendar 객체를 가져옵니다.
        Calendar service = getCalendarServiceForUser(uid);
        
        //  (최근 1주일)
        ZoneId seoulZoneId = ZoneId.of("Asia/Seoul");
//        LocalDateTime minDateTime = LocalDate.now(seoulZoneId).minusWeeks(1).atStartOfDay();
//        DateTime timeMin = new DateTime(minDateTime.atZone(seoulZoneId).toInstant().toEpochMilli());
//        LocalDateTime maxDateTime = LocalDate.now(seoulZoneId).plusDays(1).atStartOfDay();
//        DateTime timeMax = new DateTime(maxDateTime.atZone(seoulZoneId).toInstant().toEpochMilli());
        
        //(최근 1일)
//        LocalDateTime minDateTime = LocalDate.now(seoulZoneId).atStartOfDay();
//        DateTime timeMin = new DateTime(minDateTime.atZone(seoulZoneId).toInstant().toEpochMilli());
//        LocalDateTime maxDateTime = LocalDate.now(seoulZoneId).plusDays(1).atStartOfDay();
//        DateTime timeMax = new DateTime(maxDateTime.atZone(seoulZoneId).toInstant().toEpochMilli());
        //(최근 1년)
//        LocalDateTime minDateTime = LocalDate.now(seoulZoneId).minusYears(1).atStartOfDay();
//        DateTime timeMin = new DateTime(minDateTime.atZone(seoulZoneId).toInstant().toEpochMilli());
//        LocalDateTime maxDateTime = LocalDate.now(seoulZoneId).plusDays(1).atStartOfDay();
//        DateTime timeMax = new DateTime(maxDateTime.atZone(seoulZoneId).toInstant().toEpochMilli());
        //(최근 1달)
        LocalDateTime minDateTime = LocalDate.now(seoulZoneId).minusMonths(1).atStartOfDay();
        DateTime timeMin = new DateTime(minDateTime.atZone(seoulZoneId).toInstant().toEpochMilli());
        LocalDateTime maxDateTime = LocalDate.now(seoulZoneId).plusDays(1).atStartOfDay();
        DateTime timeMax = new DateTime(maxDateTime.atZone(seoulZoneId).toInstant().toEpochMilli());
        
        
        // 4. 사용자가 접근 가능한 모든 캘린더 목록 조회 및 이벤트 가져오기
        List<Event> allEvents = new ArrayList<>();
        CalendarList calendarLists = service.calendarList().list().execute();
        List<CalendarListEntry> calendars = calendarLists.getItems();
        
        if (calendars != null) {
            for (CalendarListEntry calendarEntry : calendars) {
                String calendarId = calendarEntry.getId();
                logger.info("캘린더 '{}' (ID: {})에서 일정 가져오는 중...", calendarEntry.getSummary(), calendarId);
                
                try {
                    Events events = service.events().list(calendarId)
                            .setTimeMin(timeMin)
                            .setTimeMax(timeMax)
                            .setMaxResults(20) // 캘린더당 최대 20개 이벤트 (필요시 조절)
                            .setOrderBy("startTime")
                            .setSingleEvents(true)
                            .execute();
                    
                    List<Event> items = events.getItems();
                    if (items != null && !items.isEmpty()) {
                        allEvents.addAll(items);
                    }
                } catch (IOException e) {
                    logger.error("'{}' 캘린더의 일정을 가져오는 중 오류 발생: {}", calendarId, e.getMessage());
                    // 특정 캘린더 접근 권한이 없거나 문제가 있을 수 있으므로 로그만 남기고 계속 진행
                }
            }
        }
        
        // 5. 모든 이벤트를 LocalDate별로 그룹화하여 Map으로 반환
        Map<LocalDate, List<String>> eventsByDate = allEvents.stream()
                .collect(Collectors.groupingBy(
                        event -> {
                            DateTime startDateTime = event.getStart() != null ? event.getStart().getDateTime() : null;
                            if (startDateTime == null) {
                                startDateTime = event.getStart() != null ? event.getStart().getDate() : null;
                            }
                            if (startDateTime != null) {
                                return LocalDate.ofInstant(
                                        java.time.Instant.ofEpochMilli(startDateTime.getValue()),
                                        seoulZoneId
                                );
                            } else {
                                return null;
                            }
                        },
                        Collectors.mapping(
                                event -> {
                                    String title = event.getSummary();
                                    return (title != null && !title.isEmpty()) ? title : "(제목 없음)";
                                },
                                Collectors.toList()
                        )
                ));
        
        eventsByDate.remove(null); // 날짜 정보 없는 이벤트(null 키) 제거
        
        // 6. 조회된 이벤트를 캐시에 저장
        userEventCache.put(uid, eventsByDate);
        logger.info("사용자 {}의 캘린더 이벤트를 캐시에 저장했습니다.", uid);
        
        return CompletableFuture.completedFuture(eventsByDate);
    }
    
    public boolean deleteCalendarEvents(String uid) throws IOException, GeneralSecurityException {
        if(userEventCache.containsKey(uid)) {
            userCalendarServiceCache.remove(uid);
            return true;
        }
        return false;
    }
}
