package com.shujinko.project.controller.diary;

import com.shujinko.project.domain.dto.diary.EmotionCountDto;
import com.shujinko.project.domain.dto.diary.KeywordCountDto;
import com.shujinko.project.provider.JwtTokenProvider;
import com.shujinko.project.service.diary.StatisticsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/statistics")
@SecurityRequirement(name = "Bearer Authentication")
public class StatisticsController {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final StatisticsService statisticsService;
    @Autowired
    public StatisticsController(JwtTokenProvider jwtTokenProvider, StatisticsService statisticsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.statisticsService = statisticsService;
    }
    
    
    @GetMapping("/topWeekEmotions")
    public List<EmotionCountDto> topWEmotions(Authentication authentication,@RequestParam int year, @RequestParam int month
    , @RequestParam int week) {
        String uid = authentication.getName();
        
        return statisticsService.topWeekEmotion(uid,year,month,week);
    }
    
    @GetMapping("/topMonthEmotions")
    public List<EmotionCountDto> topMEmotions(Authentication authentication,@RequestParam int year, @RequestParam int month) {
        String uid = authentication.getName();
        
        return statisticsService.topMonthEmotion(uid,year,month);
    }
    
    @GetMapping("/topWeekKeywords")
    public List<KeywordCountDto> topWKeywords(Authentication authentication, @RequestParam int year, @RequestParam int month
            , @RequestParam int week) {
        String uid = authentication.getName();
        
        return statisticsService.topWeekKeywords(uid,year,month,week);
    }
    
    @GetMapping("/topMonthKeywords")
    public List<KeywordCountDto> topMKeywords(Authentication authentication,@RequestParam int year, @RequestParam int month) {
        String uid = authentication.getName();
        
        return statisticsService.topMonthKeywords(uid,year,month);
    }

}
