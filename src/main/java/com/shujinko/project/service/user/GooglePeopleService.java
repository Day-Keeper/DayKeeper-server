package com.shujinko.project.service.user;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cglib.core.Local;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Service
public class GooglePeopleService {
    
    private final RestTemplate restTemplate;
    
    
    public GooglePeopleService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }
    
    public LocalDate getBirthdayFromGoogle(String accessToken) {
        String url = "https://people.googleapis.com/v1/people/me?personFields=birthdays";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken); // Bearer 토큰으로 Access Token 설정
        
        HttpEntity<String> entity = new HttpEntity<>("", headers);
        
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        
        JsonNode root = response.getBody();
        if (root != null && root.has("birthdays")) {
            JsonNode birthdaysNode = root.get("birthdays");
            if (birthdaysNode.isArray() && birthdaysNode.size() > 0) {
                // 첫 번째 생일 정보를 사용
                JsonNode birthdayNode = birthdaysNode.get(0).get("date");
                int year = birthdayNode.get("year").asInt();
                int month = birthdayNode.get("month").asInt();
                int day = birthdayNode.get("day").asInt();
                
                // "YYYY-MM-DD" 형식으로 반환
                return LocalDate.parse(String.format("%d-%02d-%02d", year, month, day));
            }
        }
        return null;
    }
}
