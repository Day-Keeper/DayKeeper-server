package com.shujinko.project.controller.diary;

import com.shujinko.project.provider.JwtTokenProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("/statistics")
@SecurityRequirement(name = "Bearer Authentication")
public class StatisticsController {
    
    private final JwtTokenProvider jwtTokenProvider;
    @Autowired
    public StatisticsController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }
    

}
