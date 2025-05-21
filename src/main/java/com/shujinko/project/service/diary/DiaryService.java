package com.shujinko.project.service.diary;

import com.shujinko.project.domain.dto.ai.ParagraphDto;
import com.shujinko.project.domain.dto.ai.aiResponseDto;
import com.shujinko.project.domain.dto.diary.DiaryCreateDto;
import com.shujinko.project.domain.dto.diary.DiaryRequestDto;
import com.shujinko.project.domain.dto.diary.DiaryResponseDto;
import com.shujinko.project.domain.entity.diary.Diary;
import com.shujinko.project.domain.entity.diary.DiaryEmotion;
import com.shujinko.project.domain.entity.diary.DiaryEmotionId;
import com.shujinko.project.domain.entity.diary.Emotion;
import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.repository.diary.DiaryEmotionRepository;
import com.shujinko.project.repository.diary.DiaryRepository;
import com.shujinko.project.repository.diary.EmotionRepository;
import com.shujinko.project.repository.user.UserRepository;
import com.shujinko.project.service.ai.FastApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class DiaryService {
    
    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final FastApiService fastApiService;
    private final EmotionRepository emotionRepository;
    //private final DiaryEmotionRepository diaryEmotionRepository;
    
    @Autowired
    public DiaryService(DiaryRepository diaryRepository, UserRepository userRepository,
                        FastApiService fastApiService, EmotionRepository emotionRepository,
                        DiaryEmotionRepository diaryEmotionRepository) {
        this.diaryRepository = diaryRepository;
        this.userRepository = userRepository;
        this.fastApiService = fastApiService;
        this.emotionRepository = emotionRepository;
        //this.diaryEmotionRepository = diaryEmotionRepository;
    }
    
    
    public DiaryResponseDto createDiary(DiaryCreateDto createDto,String uid){
        
        DiaryResponseDto responseDto = new DiaryResponseDto();
        Diary diary = new Diary();
        
        // <editor-fold desc="-------rephrasing diary process-----------">
        aiResponseDto aiResponse = fastApiService.callAnalyze(createDto.getRawDiary());
        
        StringBuilder rephrased = new StringBuilder();
        for(ParagraphDto p : aiResponse.getParagraphs()) {
            rephrased.append(p.getSubject());
            rephrased.append("\n");
            rephrased.append(p.getContent());
            rephrased.append("\n\n");
        }
        String rephrasedString = rephrased.toString();
        // </editor-fold>
        
        // <editor-fold desc="-------set diary information-----------">
        
        User user = userRepository.findByUid(uid);
        diary.setUser(user);
        diary.setRawDiary(createDto.getRawDiary());
        diary.setRephrasedDiary(rephrasedString);
        diary.setCreatedAt(LocalDateTime.now());
        diary.setSummary(null);
        diary.setKeywords(null);
        List<DiaryEmotion> diaryEmotions = new ArrayList<>();
        aiResponse.getEmotion().getScores().forEach((emotionName,score)->{
            DiaryEmotion element = new DiaryEmotion();
            element.setDiary(diary);
            element.setScore(score);
            
            Emotion emotion = emotionRepository.findByEmotion(emotionName);//나중에 없으면 추가 하는 로직까지?
            
            element.setEmotion(emotion);
            DiaryEmotionId id = new DiaryEmotionId();
            id.setDid(diary.getId());
            id.setEid(emotion.getId());
            element.setId(id);
            
            diaryEmotions.add(element);
        } );
        diary.setDiaryEmotions(diaryEmotions);
        diary.setLabelEmotion(aiResponse.getEmotion().getLabel());
        // </editor-fold>
        
        
        diaryRepository.save(diary);
        
        //<editor-fold desc = "-------responseDto Build-----------">
        responseDto.setRawDiary(createDto.getRawDiary());
        responseDto.setRephrasedDiary(rephrasedString);
        responseDto.setCreatedAt(LocalDateTime.now());
        responseDto.setSummary(null);
        responseDto.setKeywords(null);
        responseDto.setEmotions(aiResponse.getEmotion());
        //</editor-fold>
        
        return responseDto;
    }
    
    public List<DiaryResponseDto> getAllDiaries(DiaryRequestDto requestDto,String uid){
        
        User user = userRepository.findByUid(uid);
        
        LocalDateTime start = YearMonth.of(requestDto.getYear(),requestDto.getMonth())
                .atDay(1).atStartOfDay();
        LocalDateTime end = YearMonth.of(requestDto.getYear(),requestDto.getMonth())
                .atEndOfMonth().atTime(23, 59, 59, 999_999_999);
        List<Diary> diaries = diaryRepository.findByUser_UidAndCreatedAtBetween(uid, start, end);
        
        
        // <editor-fold desc="build up DiaryResponseDto list">
        List<DiaryResponseDto> result = new ArrayList<>();
        diaries.forEach(diary -> {
            DiaryResponseDto responseDto = new DiaryResponseDto();
            responseDto.setRawDiary(diary.getRawDiary());
            responseDto.setRephrasedDiary(diary.getRephrasedDiary());
            responseDto.setCreatedAt(diary.getCreatedAt());
            System.out.println();
            result.add(responseDto);
        });
        // </editor-fold>
        
        
        return result;
    }
}