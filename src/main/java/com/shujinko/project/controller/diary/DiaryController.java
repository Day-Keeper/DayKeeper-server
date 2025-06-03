package com.shujinko.project.controller.diary;


import com.shujinko.project.domain.dto.diary.DiaryCreateDto;
import com.shujinko.project.domain.dto.diary.DiaryRequestDto;
import com.shujinko.project.domain.dto.diary.DiaryResponseDto;
import com.shujinko.project.domain.dto.diary.DiaryUpdateDto;
import com.shujinko.project.provider.JwtTokenProvider;
import com.shujinko.project.service.diary.DiaryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/diary")
@SecurityRequirement(name = "Bearer Authentication")
public class DiaryController {
    
    private final Logger logger = LoggerFactory.getLogger(DiaryController.class);
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
    public ResponseEntity<DiaryResponseDto> getDiary(Authentication authentication,
                                     @RequestParam("year") int year,
                                     @RequestParam("month") int month,
                                     @RequestParam("day") int day){
        DiaryRequestDto request = DiaryRequestDto.builder().year(year).month(month).day(day).build();
        String uid = authentication.getName();
        Optional<DiaryResponseDto> diaryDtoOptional = diaryService.getDiary(request,uid);
        return diaryDtoOptional
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
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
    
    @PostMapping(value = "/photoDiary", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DiaryResponseDto> createPhotoDiary(Authentication authentication,
                                             @RequestPart DiaryCreateDto diaryCreateDto,
                                             @RequestPart("photo")List<MultipartFile> photoFile){
        
        return null;
    }
    
    @GetMapping("/reset")
    public void reset(Authentication authentication) {
        String uid = authentication.getName();
        diaryService.resetStat(uid);
        
    }
    
}
