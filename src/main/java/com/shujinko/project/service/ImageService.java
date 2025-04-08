package com.shujinko.project.service;

import com.shujinko.project.domain.entity.Image;
import com.shujinko.project.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
public class ImageService {
    
    ImageRepository imageRepository;
    
    @Autowired
    ImageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }
    
    public String saveImage(MultipartFile file) throws IOException {
        
        String uploadDir = "/home/ec2-user/images/";
        String filePath = uploadDir + file.getOriginalFilename();
        
        File dest = new File(filePath);
        if(imageRepository.findByUrl(filePath)!=null){
            return "Image already exists";
        }
        file.transferTo(dest);
        Image image = Image.builder().
                url(filePath).build();
        imageRepository.save(image);
        return "Image saved";
    }
}
