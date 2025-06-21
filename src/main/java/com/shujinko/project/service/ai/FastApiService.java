package com.shujinko.project.service.ai;

import com.shujinko.project.domain.dto.ai.AiResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Objects;

@Service
public class FastApiService {
    
    private final WebClient webClient;
    private final static String url = "http://58.76.169.75:8000";
    
    @Autowired
    public FastApiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(url).build();
    }
    
    public AiResponseDto callAnalyze(String input, Map<String, MultipartFile> photos) {
        // HttpHeaders는 WebClient.contentType()에서 자동으로 설정되므로, 여기서 수동으로 설정할 필요는 없습니다.
        // 하지만 MultipartBodyBuilder 자체는 각 파트의 Content-Type을 설정할 수 있습니다.
        
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        
        // 텍스트 파트 추가
        builder.part("text", input);
        
        // 사진 파일 파트 추가
        photos.forEach((fileName, photo) -> {
            if (photo.isEmpty()) {
                // 비어있는 파일은 건너뜀
                return;
            }
            
            // MultipartFile에서 가져온 원본 Content-Type을 저장
            String originalContentType = photo.getContentType();
            MediaType mediaTypeToUse;
            
            // 1. 원본 Content-Type이 null이거나 와일드카드를 포함하는지 확인
            if (originalContentType == null || originalContentType.contains("*")) {
                // 와일드카드(`*`)가 포함된 경우
                System.out.println("경고: 받은 Content-Type '" + originalContentType + "'에 와일드카드 문자가 포함되어 있습니다. 구체적인 타입으로 대체합니다.");
                
                // 'image/*'와 같은 경우, 기본적으로 'image/jpeg'로 설정하거나,
                // 더 일반적인 바이너리 스트림 타입인 'application/octet-stream'을 사용할 수 있습니다.
                // 여기서는 안전하게 'application/octet-stream'을 사용합니다.
                // 만약 FastAPI 서버가 엄격하게 이미지 타입만 받는다면 image/jpeg 등을 고려하세요.
                mediaTypeToUse = MediaType.APPLICATION_OCTET_STREAM; // 안전한 기본값
                
                // 또는, 이미지 파일이라면 이렇게 특정 이미지 타입으로 지정할 수도 있습니다.
                // if (originalContentType != null && originalContentType.toLowerCase().startsWith("image/")) {
                //     mediaTypeToUse = MediaType.IMAGE_JPEG; // 예시: JPG로 기본 설정
                // } else {
                //     mediaTypeToUse = MediaType.APPLICATION_OCTET_STREAM;
                // }
                
            } else {
                // 와일드카드가 없는 유효한 Content-Type인 경우 그대로 사용
                mediaTypeToUse = MediaType.parseMediaType(originalContentType);
            }
            
            // MultipartBodyBuilder에 파일 파트 추가
            // 각 파일의 실제 Content-Type을 명시적으로 추가
            builder.part("images", photo.getResource())
                    // Content-Disposition 헤더는 파일 이름 등을 명시하여 수신자가 파일 정보를 파악할 수 있도록 돕습니다.
                    .header("Content-Disposition", "form-data; name=images; filename=" + fileName)
                    // 수정된 mediaTypeToUse를 사용하여 Content-Type 설정
                    .contentType(mediaTypeToUse);
        });
        
        // WebClient를 사용하여 FastAPI 서비스 호출
        return webClient.post()
                .uri("/analyze")
                // 요청의 전체 Content-Type을 MULTIPART_FORM_DATA로 설정
                .contentType(MediaType.MULTIPART_FORM_DATA)
                // builder로 생성된 multipart 데이터를 요청 본문으로 설정
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                // 응답을 AiResponseDto 클래스로 매핑
                .bodyToMono(AiResponseDto.class)
                .block(); // 비동기 호출을 동기적으로 블로킹 (실제 서비스에서는 비동기 처리 권장)
    }
}