package com.shujinko.project.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shujinko.project.domain.dto.ai.ParagraphDto;
import com.shujinko.project.domain.dto.ai.aiResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class FastApiService {
    
    private final ObjectMapper mapper;
    private final static String url = "http://52.79.171.211:8000/analyze";
    
    @Autowired
    public FastApiService(ObjectMapper mapper) {
        this.mapper = mapper;
    }
    
    public aiResponseDto callAnalyze(String input){
        
        // <editor-fold desc="Json request body">
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text",input);
        // </editor-fold>
        
        // <editor-fold desc="header configuration">
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String,String>> requestEntity = new HttpEntity<>(requestBody, headers);
        // </editor-fold>
        
        // <editor-fold desc="send request">
        RestTemplate restTemplate = new RestTemplate();
        
        ResponseEntity<String> response = null;
        aiResponseDto result = null;
        try{
            System.out.println("요청 URL: " + url);
            long startTime = System.nanoTime(); // 요청 보내기 직전 시간 기록
            response = restTemplate.postForEntity(url, requestEntity, String.class);
            long endTime = System.nanoTime(); // 응답 받은 직후 시간 기록
            long durationNano = endTime - startTime;
            double durationSeconds = (double) durationNano / 1_000_000_000.0;
            System.out.printf("AI 응답 수신 시간: %.3f 초%n", durationSeconds); // 초 단위로 출력
            System.out.println("Raw 응답 문자열: " + response);
            
            result = mapper.readValue(response.getBody(), aiResponseDto.class);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
        // </editor-fold>
        
        
        return result;
        // </editor-fold>
    }
}
