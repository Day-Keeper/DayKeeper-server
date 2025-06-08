package com.shujinko.project.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shujinko.project.domain.dto.ai.ParagraphDto;
import com.shujinko.project.domain.dto.ai.aiResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class FastApiService {
    
    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final static String url = "http://34.45.39.185:8000";
    
    @Autowired
    public FastApiService(ObjectMapper mapper, WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(url).build();
        this.mapper = mapper;
    }
    
    public aiResponseDto callAnalyze(String input, Map<String,MultipartFile> photos){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();
        
        body.add("text", input);
        
        photos.forEach((fileName,photo)->{
            try{
                ByteArrayResource image =new ByteArrayResource(photo.getBytes()) {
                    @Override
                    public String getFilename() {
                        return fileName;
                    }
                };
                body.add("images", image);
            }catch(IOException e){
                throw new RuntimeException("Error handling photo");
            }
        });
        
        HttpEntity<MultiValueMap<String,Object>> requestEntity = new HttpEntity<>(body, headers);
        
        return webClient.post()
                .uri("/analyze")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(aiResponseDto.class)
                .block();
    }
}
