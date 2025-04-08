package com.shujinko.project.domain.dto;

import com.shujinko.project.domain.entity.Image;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ImageDto {
    Long image_Id;
    String filename;
    LocalDateTime takentime;
    
    public Image toImage() {
        return Image.builder().
                url("/home/ec2-user/images/"+filename).
                takentime(takentime).
                build();
    }
}
