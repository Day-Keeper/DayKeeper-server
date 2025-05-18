package com.shujinko.project.service.diary;

import com.shujinko.project.domain.dto.diary.DiaryCreateDto;
import com.shujinko.project.domain.dto.diary.DiaryResponseDto;
import com.shujinko.project.domain.entity.diary.Diary;
import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.repository.diary.DiaryRepository;
import com.shujinko.project.repository.user.UserRepository;
import com.shujinko.project.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class DiaryService {
    
    @Autowired
    private DiaryRepository diaryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public DiaryResponseDto createDiary(DiaryCreateDto createDto,String uid){
        // <editor-fold desc="-------set diary information-----------">
        Diary diary = new Diary();
        User user = userRepository.findByUid(uid);
        diary.setUser(user);
        diary.setCreatedAt(LocalDateTime.now());
        diary.setRawDiary(createDto.getRawDiary());
        // </editor-fold>
        
        // <editor-fold desc="-------rephrasing diary process-----------">
        diary.setRephrasedDiary("rephrased diary of \n" + createDto.getRawDiary());
        // </editor-fold>
        
        diaryRepository.save(diary);
        
        //<editor-fold desc = "-------responseDto Build-----------">
        DiaryResponseDto responseDto = new DiaryResponseDto();
        responseDto.setRawDiary(diary.getRawDiary());
        responseDto.setRephrasedDiary(diary.getRephrasedDiary());
        responseDto.setCreatedAt(diary.getCreatedAt());
        responseDto.setRawDiary(null);
        responseDto.setRephrasedDiary(null);
        responseDto.setEmotions(null);
        //</editor-fold>
        
        return responseDto;
    }
    /*
    public List<DiaryResponseDto> getAllDiaries(DiaryRequestDto requestDto){
        LocalDateTime start = YearMonth.of(requestDto.getYear(),requestDto.getMonth())
                .atDay(1).atStartOfDay();
        LocalDateTime end = YearMonth.of(requestDto.getYear(),requestDto.getMonth())
                .atEndOfMonth().atTime(23, 59, 59, 999_999_999);
        List<Diary> diaries = diaryRepository.findByUserIdAndCreatedAtBetween(requestDto.getUserId(), start, end);
        
        
        // <editor-fold desc="build up DiaryResponseDto list">
        List<DiaryResponseDto> result = new ArrayList<>();
        diaries.forEach(diary -> {
            DiaryResponseDto responseDto = new DiaryResponseDto();
            responseDto.setTitle(diary.getTitle());
            responseDto.setRawDiary(diary.getRawDiary());
            responseDto.setRephrasedDiary(diary.getRephrasedDiary());
            responseDto.setCreatedAt(diary.getCreatedAt());
            System.out.println();
            result.add(responseDto);
        });
        // </editor-fold>
        
        
        return result;
    }*/
}