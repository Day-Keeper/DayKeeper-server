package com.shujinko.project.controller;

import com.shujinko.project.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/images")
public class ImagerController {
    
    ImageService imageService;
    
    @Autowired
    public ImagerController(ImageService imageService) {
        this.imageService = imageService;
    }
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadImage(
            @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(imageService.saveImage(file));
    }
    

}
