package com.shujinko.project.controller.diary;

import com.shujinko.project.domain.dto.diary.EmotionCountDto;
import com.shujinko.project.provider.JwtTokenProvider;
import com.shujinko.project.service.diary.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final StatisticsService statisticsService;
    @Autowired
    public StatisticsController(JwtTokenProvider jwtTokenProvider, StatisticsService statisticsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.statisticsService = statisticsService;
    }
    
    
    @GetMapping("/top3WeekEmotions")
    public List<EmotionCountDto> top3WEmotions(Authentication authentication,@RequestParam int year, @RequestParam int month
    , @RequestParam int week) {
        String uid = authentication.getName();
        
        return statisticsService.top3WeekEmotion(uid,year,month,week);
    }
    
    @GetMapping("/top3MonthEmotions")
    public List<EmotionCountDto> top3MEmotions(Authentication authentication,@RequestParam int year, @RequestParam int month) {
        String uid = authentication.getName();
        
        return statisticsService.top3MothEmotion(uid,year,month);
    }

}
