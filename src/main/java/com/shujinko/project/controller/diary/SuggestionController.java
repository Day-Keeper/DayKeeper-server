package com.shujinko.project.controller.diary;


import com.shujinko.project.domain.dto.ai.GeminiSuggestion;
import com.shujinko.project.domain.dto.ai.SuggestionRequest;
import com.shujinko.project.domain.dto.diary.DiaryCreateDto;
import com.shujinko.project.service.diary.DiarySuggestionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/suggestion")
@SecurityRequirement(name = "Bearer Authentication")
public class SuggestionController {

    DiarySuggestionService diarySuggestionService;

    @Autowired
    public SuggestionController(DiarySuggestionService diarySuggestionService) {
        this.diarySuggestionService = diarySuggestionService;
    }
    @PostMapping
    public GeminiSuggestion getSuggestion(Authentication authentication, @RequestBody SuggestionRequest request) throws Exception {
        String uid = authentication.getName();
        return diarySuggestionService.getDiarySuggestion(uid, request);
    }
    
}
