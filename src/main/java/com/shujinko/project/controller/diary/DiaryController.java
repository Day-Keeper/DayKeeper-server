package com.shujinko.project.controller.diary;


import com.shujinko.project.domain.dto.diary.DiaryCreateDto;
import com.shujinko.project.domain.dto.diary.DiaryRequestDto;
import com.shujinko.project.domain.dto.diary.DiaryResponseDto;
import com.shujinko.project.domain.dto.diary.DiaryUpdateDto;
import com.shujinko.project.provider.JwtTokenProvider;
import com.shujinko.project.service.diary.DiaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/diary")
public class DiaryController {
    
    JwtTokenProvider jwtTokenProvider;
    DiaryService diaryService;
    @Autowired
    public DiaryController(final JwtTokenProvider jwtTokenProvider, final DiaryService diaryService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.diaryService = diaryService;
    }
    
    @PostMapping("")
    public DiaryResponseDto createDiary(
            Authentication authentication,
            @RequestBody DiaryCreateDto diaryCreateDto) {
        String uid = authentication.getName();
        String rawDiary = diaryCreateDto.getRawDiary();
        return diaryService.createDiary(diaryCreateDto,uid);
    }
    
    @GetMapping("/diaries")
    public List<DiaryResponseDto> getDiaries(Authentication authentication,
                                             @RequestParam("year") int year
    , @RequestParam("month") int month) {
        DiaryRequestDto request = new DiaryRequestDto();
        request.setYear(year);
        request.setMonth(month);
        request.setDay(0);
        String uid = authentication.getName();
        return diaryService.getAllDiaries(request,uid);
    }
    
    @GetMapping("")
    public List<DiaryResponseDto> getDiary(Authentication authentication,
                                     @RequestParam("year") int year,
                                     @RequestParam("month") int month,
                                     @RequestParam("day") int day){
        DiaryRequestDto request = DiaryRequestDto.builder().year(year).month(month).day(day).build();
        String uid = authentication.getName();
        return diaryService.getDiary(request,uid);
    }
    
    @DeleteMapping("/{id}")
    public void deleteDiary(Authentication authentication,
                            @PathVariable Long id) throws Exception{
        String uid = authentication.getName();
        diaryService.deleteDiary(id,uid);
    }
    
    @PatchMapping("/{id}")
    public DiaryResponseDto updateDiary(Authentication authentication,
                                        @PathVariable Long id
                                        , @RequestBody DiaryUpdateDto diaryUpdateDto) throws Exception{
        String uid = authentication.getName();
        return diaryService.updateDiary(diaryUpdateDto,id,uid);
    }
    
}
