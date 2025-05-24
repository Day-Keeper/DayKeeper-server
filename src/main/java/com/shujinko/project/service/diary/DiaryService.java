package com.shujinko.project.service.diary;

import com.shujinko.project.domain.dto.ai.*;
import com.shujinko.project.domain.dto.diary.DiaryCreateDto;
import com.shujinko.project.domain.dto.diary.DiaryRequestDto;
import com.shujinko.project.domain.dto.diary.DiaryResponseDto;
import com.shujinko.project.domain.entity.diary.*;
import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.repository.diary.*;
import com.shujinko.project.repository.user.UserRepository;
import com.shujinko.project.service.ai.FastApiService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DiaryService {
    
    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final FastApiService fastApiService;
    private final EmotionRepository emotionRepository;
    private final KeywordRepository keywordRepository;
    private final DiaryEmotionRepository diaryEmotionRepository;
    private final DiaryKeywordRepository diaryKeywordRepository;
    
    @Autowired
    public DiaryService(DiaryRepository diaryRepository, UserRepository userRepository,
                        FastApiService fastApiService, EmotionRepository emotionRepository,
                        DiaryEmotionRepository diaryEmotionRepository, KeywordRepository keywordRepository, DiaryKeywordRepository diaryKeywordRepository) {
        this.diaryRepository = diaryRepository;
        this.userRepository = userRepository;
        this.fastApiService = fastApiService;
        this.emotionRepository = emotionRepository;
        this.diaryEmotionRepository = diaryEmotionRepository;
        this.keywordRepository = keywordRepository;
        this.diaryKeywordRepository = diaryKeywordRepository;
    }
    
    @Transactional
    public DiaryResponseDto createDiary(DiaryCreateDto createDto,String uid){
        DiaryResponseDto responseDto = new DiaryResponseDto();
        aiResponseDto aiResponse = fastApiService.callAnalyze(createDto.getRawDiary());
        String rephrasedString = getResult(aiResponse);//일기 정리
        User user = userRepository.findByUid(uid);
        Diary diary = Diary.builder().
                user(user).
                rawDiary(createDto.getRawDiary()).
                rephrasedDiary(rephrasedString).
                createdAt(LocalDateTime.now()).
                summary(null).
                LabelEmotion(aiResponse.getEmotion().getLabel()).build();
        diary = diaryRepository.save(diary);
        
        
// <editor-fold desc="감정 처리">
        Map<String,Double> aiEmotionScores = aiResponse.getEmotion().getScores();
        List<String> emotionNames = new ArrayList<>(aiEmotionScores.keySet());
        List<Emotion> foundEmotions = emotionRepository.findByEmotionStrIn(emotionNames);
        /*존재하는 감정 맵*/
        Map<String,Emotion> ExisitingEmotionMap = foundEmotions.stream()
                .collect(Collectors.toMap(Emotion::getEmotionStr, Function.identity()));
        
        List<Emotion> newEmotions = emotionNames.stream().filter(emotion-> !ExisitingEmotionMap.containsKey(emotion))
                .map(emotion -> {
                    Emotion newEmotion = new Emotion();
                    newEmotion.setEmotionStr(emotion);
                    return newEmotion;
                }).toList();
        
        if(!newEmotions.isEmpty()){
            emotionRepository.saveAll(newEmotions);
            for(Emotion e : newEmotions){
                ExisitingEmotionMap.put(e.getEmotionStr(), e);
            }
        }
        
        List<DiaryEmotion> savingDiaryEmotions = new ArrayList<>();
        List<EmotionScoreDto> responsingEmotionScoreDtos = new ArrayList<>();
        for(Map.Entry<String,Double> entry : aiEmotionScores.entrySet()){
            Emotion emotion = ExisitingEmotionMap.get(entry.getKey());
            DiaryEmotion de = new DiaryEmotion(
                    new DiaryEmotionId(diary.getId(),emotion.getId()),
                    diary,
                    emotion,
                    entry.getValue()
            );
            savingDiaryEmotions.add(de);
            responsingEmotionScoreDtos.add(new EmotionScoreDto(entry.getKey(),entry.getValue()));
        }
        diaryEmotionRepository.saveAll(savingDiaryEmotions);
// </editor-fold>
        List<String> keywordStrings = aiResponse.getKeywords().stream()
                .map(aiKeywordDto::getText)
                .toList();
        List<Keyword> foundKeywords = keywordRepository.findByKeywordStrIn(keywordStrings);
        Map<String,Keyword> ExistingKeywordMap = foundKeywords.stream()
                .collect(Collectors.toMap(Keyword::getKeywordStr, Function.identity()));
        
        List<Keyword> newKeywords = keywordStrings.stream()
                .filter(kw -> !ExistingKeywordMap.containsKey(kw))
                .map(kw -> {
                    Keyword newKeyword = new Keyword();
                    newKeyword.setKeywordStr(kw);
                    return newKeyword;
                })
                .toList();
        
        if(!newKeywords.isEmpty()){
            keywordRepository.saveAll(newKeywords);
            for (Keyword k : newKeywords) {
                ExistingKeywordMap.put(k.getKeywordStr(), k);
            }
        }
        List<DiaryKeyword> savingDiaryKeywords = new ArrayList<>();
        for (String kw : keywordStrings) {
            Keyword k = ExistingKeywordMap.get(kw);
            DiaryKeywordId dkId = new DiaryKeywordId(diary.getId(), k.getId());
            DiaryKeyword dk = new DiaryKeyword(dkId, diary, k);
            savingDiaryKeywords.add(dk);
        }
        diaryKeywordRepository.saveAll(savingDiaryKeywords);
        
        // </editor-fold>
        
        //<editor-fold desc = "-------responseDto 빌드-----------">
        responseDto.setRawDiary(createDto.getRawDiary());
        responseDto.setRephrasedDiary(rephrasedString);
        responseDto.setCreatedAt(LocalDateTime.now());
        responseDto.setSummary(null);
        responseDto.setKeywords(keywordStrings);
        responseDto.setEmotions(responsingEmotionScoreDtos);
        responseDto.setLabel(aiResponse.getEmotion().getLabel());
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
        diaries.forEach(diary -> {//각 다이어리에 대해서
            DiaryResponseDto responseDto = DiaryResponseDto.builder()
                    .rawDiary(diary.getRawDiary())
                    .rephrasedDiary(diary.getRephrasedDiary())
                    .createdAt(diary.getCreatedAt())
                    .summary(diary.getSummary())
                    .label(diary.getLabelEmotion())
                    .keywords(null)
                    .build();
            List<EmotionScoreDto> emotionScoreDtos = getEmotionScoreDtos(diary);
            responseDto.setEmotions(emotionScoreDtos);
            result.add(responseDto);
        });
        // </editor-fold>
        return result;
    }
    
    public List<DiaryResponseDto> getDiary(DiaryRequestDto requestDto,String uid){
        User user = userRepository.findByUid(uid);
        LocalDate date = LocalDate.of(requestDto.getYear(),requestDto.getMonth(),requestDto.getDay());
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return diaryRepository.findByUser_UidAndCreatedAtBetween(uid,start,end).stream().map(Diary::toResponseDto).collect(Collectors.toList());
    }
    
    
    /*--------------------유틸리티 함수-----------------*/
    
    private static String getResult(aiResponseDto aiResponse) {
        StringBuilder rephrased = new StringBuilder();
        for(ParagraphDto p : aiResponse.getParagraphs()) {
            rephrased.append(p.getSubject());
            rephrased.append("\n");
            rephrased.append(p.getContent());
            rephrased.append("\n\n");
        }
        return rephrased.toString();
    }
    
    private static List<EmotionScoreDto> getEmotionScoreDtos(Diary diary) {
        List<DiaryEmotion> diaryEmotions = diary.getDiaryEmotions();
        List<EmotionScoreDto> emotionScoreDtos = new ArrayList<>();
        
        diaryEmotions.forEach(diaryEmotion -> {
            EmotionScoreDto emotionScoreDto = EmotionScoreDto.builder().
                    score(diaryEmotion.getScore()).
                    emotion(diaryEmotion.getEmotion().getEmotionStr()).
                    build();
            emotionScoreDtos.add(emotionScoreDto);
        });
        return emotionScoreDtos;
    }
}
