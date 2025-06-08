package com.shujinko.project.service.diary;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ImageServingService {
    
    @Value("${file.upload-dir}")
    private String uploadDir;
    
    public Resource loadImage(String filename) throws IOException {
        Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
        if(!Files.exists(filePath)) {
            throw new IOException(filename + " does not exist");
        }
        
        return new ByteArrayResource(Files.readAllBytes(filePath));
    }
    
    public MediaType getMediaTypeForFileName(String fileName) {
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (fileExtension) {
            case "jpeg":
            case "jpg":
                return MediaType.IMAGE_JPEG;
            case "png":
                return MediaType.IMAGE_PNG;
            case "gif":
                return MediaType.IMAGE_GIF;
            // 필요에 따라 다른 이미지 포맷 추가
            default:
                return MediaType.APPLICATION_OCTET_STREAM; // 알 수 없는 타입은 이진 데이터로 처리
        }
    }
    
    public void saveImage(MultipartFile photo, String fileName) throws IOException{
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
        Path targetLocation = uploadPath.resolve(fileName);
        Files.copy(photo.getInputStream(), targetLocation);
    }
}
