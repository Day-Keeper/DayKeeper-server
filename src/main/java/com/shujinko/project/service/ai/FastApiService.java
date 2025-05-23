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
    private final static String url = "https://woodcock-regular-partially.ngrok-free.app/analyze";
    
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
            response = restTemplate.postForEntity(url, requestEntity, String.class);
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
