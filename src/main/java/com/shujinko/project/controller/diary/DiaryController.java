package com.shujinko.project.controller.diary;


import com.shujinko.project.domain.dto.diary.DiaryCreateDto;
import com.shujinko.project.domain.dto.diary.DiaryRequestDto;
import com.shujinko.project.domain.dto.diary.DiaryResponseDto;
import com.shujinko.project.domain.dto.diary.DiaryUpdateDto;
import com.shujinko.project.provider.JwtTokenProvider;
import com.shujinko.project.service.diary.DiaryService;
import com.shujinko.project.service.diary.ImageServingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/diary")
@SecurityRequirement(name = "Bearer Authentication")
public class DiaryController {
    
    private final Logger logger = LoggerFactory.getLogger(DiaryController.class);
    JwtTokenProvider jwtTokenProvider;
    DiaryService diaryService;
    ImageServingService imageServingService;
    @Autowired
    public DiaryController(final JwtTokenProvider jwtTokenProvider, final DiaryService diaryService,final ImageServingService imageServingService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.diaryService = diaryService;
        this.imageServingService = imageServingService;
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
    
    
    @PostMapping(value = "/photoDiary", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DiaryResponseDto createPhotoDiary(Authentication authentication,
                                             @RequestPart("createParam") DiaryCreateDto diaryCreateDto,
                                             @RequestPart(value = "images",required = false)List<MultipartFile> photoFile) throws IOException{
        String uid = authentication.getName();
        return diaryService.createPhotoDiary(uid,diaryCreateDto,photoFile);
    }
    
    @PatchMapping(value = "/photoDiary/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DiaryResponseDto updatePhotoDiary(Authentication authentication,
                                             @PathVariable Long id,
                                             @RequestPart("updateParam") DiaryUpdateDto diaryUpdateDto,
                                             @RequestPart(value = "images",required = false)List<MultipartFile> photoFile) throws IOException{
        String uid = authentication.getName();
        return diaryService.updateDiary(diaryUpdateDto,id,uid,photoFile);
    }
    
    @GetMapping("/images/{fileName:.+}")
    public ResponseEntity<Resource> serveImage(Authentication authentication, @PathVariable String fileName) throws IOException{
        try {
            Resource imageResource = imageServingService.loadImage(fileName);
            MediaType contentType = imageServingService.getMediaTypeForFileName(fileName);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(contentType);
            headers.setCacheControl("max-age=3600");
            return new ResponseEntity<>(imageResource, headers, HttpStatus.OK);
        }catch(IOException e){
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/reset")
    public void reset(Authentication authentication) throws Exception {
        String uid = authentication.getName();
        diaryService.resetStat(uid);
    }
    
}
