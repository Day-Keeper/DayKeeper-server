package com.shujinko.project.service.diary;

import com.shujinko.project.domain.dto.ai.EmotionResultDto;
import com.shujinko.project.domain.dto.ai.EmotionScoreDto;
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
    private final DiaryEmotionRepository diaryEmotionRepository;
    
    @Autowired
    public DiaryService(DiaryRepository diaryRepository, UserRepository userRepository,
                        FastApiService fastApiService, EmotionRepository emotionRepository,
                        DiaryEmotionRepository diaryEmotionRepository) {
        this.diaryRepository = diaryRepository;
        this.userRepository = userRepository;
        this.fastApiService = fastApiService;
        this.emotionRepository = emotionRepository;
        this.diaryEmotionRepository = diaryEmotionRepository;
    }
    
    
    public DiaryResponseDto createDiary(DiaryCreateDto createDto,String uid){
        
        DiaryResponseDto responseDto = new DiaryResponseDto();
        Diary diary = new Diary();
        
        // <editor-fold desc="-------일기 정리하기-----------">
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
        
// <editor-fold desc="감정 정리">
        List<DiaryEmotion> diaryEmotions = new ArrayList<>();
        List<EmotionScoreDto> emotionScores = new ArrayList<>();
        aiResponse.getEmotion().getScores().forEach((emotion,score)->{
            DiaryEmotion de = new DiaryEmotion();
            EmotionScoreDto es = new EmotionScoreDto();
            de.setDiary(diary);
            de.setScore(score);
            es.setEmotion(emotion);
            es.setScore(score);
            
            Emotion resEmotion = emotionRepository.findByEmotion(emotion);//나중에 없으면 추가 하는 로직까지?
            de.setEmotion(resEmotion);
            
            DiaryEmotionId deId = new DiaryEmotionId();
            deId.setDid(diary.getId());
            deId.setEid(resEmotion.getId());
            de.setId(deId);
            
            emotionScores.add(es);
            diaryEmotions.add(de);
        } );
        diary.setDiaryEmotions(diaryEmotions);
        diary.setLabelEmotion(aiResponse.getEmotion().getLabel());
// </editor-fold>

        // </editor-fold>
        
        
        diaryRepository.save(diary);
        
        //<editor-fold desc = "-------responseDto 빌드-----------">
        responseDto.setRawDiary(createDto.getRawDiary());
        responseDto.setRephrasedDiary(rephrasedString);
        responseDto.setCreatedAt(LocalDateTime.now());
        responseDto.setSummary(null);
        responseDto.setKeywords(null);
        responseDto.setEmotions(emotionScores);
        responseDto.setLabel(aiResponse.getEmotion().getLabel());
        //</editor-fold>
        
        return responseDto;
    }
    
    public List<DiaryResponseDto> createDiary(DiaryCreateDto createDto){
        return null;
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
        diaries.forEach(diary -> {//각 다이어리에 대해서
            DiaryResponseDto responseDto = new DiaryResponseDto();
            responseDto.setRawDiary(diary.getRawDiary());
            responseDto.setRephrasedDiary(diary.getRephrasedDiary());
            responseDto.setCreatedAt(diary.getCreatedAt());
            responseDto.setSummary(diary.getSummary());
            responseDto.setLabel(diary.getLabelEmotion());
            responseDto.setKeywords(null);
            
            List<DiaryEmotion> diaryEmotions = diary.getDiaryEmotions();
            List<EmotionScoreDto> emotionScoreDtos = new ArrayList<>();
            
            diaryEmotions.forEach(diaryEmotion -> {
                EmotionScoreDto emotionScoreDto = new EmotionScoreDto();
                emotionScoreDto.setScore(diaryEmotion.getScore());
                emotionScoreDto.setEmotion(diaryEmotion.getEmotion().getEmotion());
                emotionScoreDtos.add(emotionScoreDto);
            });
            responseDto.setEmotions(emotionScoreDtos);
            
            result.add(responseDto);
        });
        // </editor-fold>
        
        
        return result;
    }
    
}