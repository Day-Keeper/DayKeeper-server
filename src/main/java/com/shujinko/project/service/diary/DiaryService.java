package com.shujinko.project.service.diary;

import com.shujinko.project.domain.dto.ai.*;
import com.shujinko.project.domain.dto.diary.DiaryCreateDto;
import com.shujinko.project.domain.dto.diary.DiaryRequestDto;
import com.shujinko.project.domain.dto.diary.DiaryResponseDto;
import com.shujinko.project.domain.dto.diary.DiaryUpdateDto;
import com.shujinko.project.domain.entity.diary.*;
import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.repository.diary.*;
import com.shujinko.project.repository.user.UserRepository;
import com.shujinko.project.service.ai.FastApiService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.AccessDeniedException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DiaryService {
    
    private static final Logger logger = LoggerFactory.getLogger(DiaryService.class);
    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final FastApiService fastApiService;
    private final EmotionRepository emotionRepository;
    private final KeywordRepository keywordRepository;
    private final StatisticsService statisticsService;
    private final WeeklyKeywordStatRepository weeklyKeywordStatRepository;
    
    @Autowired
    public DiaryService(DiaryRepository diaryRepository, UserRepository userRepository,
                        FastApiService fastApiService, EmotionRepository emotionRepository,
                        KeywordRepository keywordRepository,StatisticsService statisticsService, WeeklyKeywordStatRepository weeklyKeywordStatRepository) {
        this.diaryRepository = diaryRepository;
        this.userRepository = userRepository;
        this.fastApiService = fastApiService;
        this.emotionRepository = emotionRepository;
        this.keywordRepository = keywordRepository;
        this.statisticsService = statisticsService;
        this.weeklyKeywordStatRepository = weeklyKeywordStatRepository;
    }
    
    @Transactional
    public DiaryResponseDto createDiary(DiaryCreateDto createDto, String uid) {
        long overallStartTime = System.nanoTime();
        //유저 찾기
        User user = findUserByUid(uid);
        //날짜 파싱
        LocalDate diaryDate = LocalDate.parse(createDto.getDiaryDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        //해당 날짜 일기 없는지 찾기
        validateDiaryDoesNotExistForDate(uid, diaryDate);
        //AI 응답
        long aiStartTime = System.nanoTime();
        aiResponseDto aiResponse = fastApiService.callAnalyze(createDto.getRawDiary());
        double secs = logAiProcessingTime(aiStartTime);
        //정제된 일기 줄바꿈
        String rephrasedString = getResult(aiResponse);
        //save(diary)
        Diary diary = createAndSaveInitialDiary(user, createDto.getRawDiary(), rephrasedString, diaryDate.atTime(11,0), aiResponse.getEmotion().getLabel(),aiResponse.getSummary());
        //Emotion 저장
        processAndLinkEmotions(diary, aiResponse.getEmotion().getScores());
        //Keyword 저장
        processAndLinkKeywords(diary, aiResponse.getKeywords());
        //주간 통계
        statisticsService.updateKeywordStatistics(uid, diaryDate);
        
        long endTime = System.nanoTime();
        double duration = (endTime - overallStartTime) / 1_000_000_000.0;
        double dur = duration - secs;
        logger.info("----------------------Diary creation time : [{}]-------------------",dur);
        return diary.toResponseDto();
    }
    
    
    public List<DiaryResponseDto> getAllDiaries(DiaryRequestDto requestDto,String uid){
        
        User user = userRepository.findByUid(uid);
        
        LocalDateTime start = YearMonth.of(requestDto.getYear(),requestDto.getMonth())
                .atDay(1).atStartOfDay();
        LocalDateTime end = YearMonth.of(requestDto.getYear(),requestDto.getMonth())
                .atEndOfMonth().atTime(23, 59, 59, 999_999_999);
        List<Diary> diaries = diaryRepository.findByUser_UidAndCreatedAtBetween(uid, start, end);
        return diaryRepository.findByUser_UidAndCreatedAtBetween(uid,start,end).stream().map(Diary::toResponseDto).collect(Collectors.toList());
    }
    
    public Optional<DiaryResponseDto> getDiary(DiaryRequestDto requestDto,String uid){
        User user = userRepository.findByUid(uid);
        LocalDate date = LocalDate.of(requestDto.getYear(),requestDto.getMonth(),requestDto.getDay());
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return diaryRepository.findByUser_UidAndCreatedAtBetween(uid,start,end).stream().map(Diary::toResponseDto).toList().stream().findFirst();
    }
    
    @Transactional
    public String deleteDiary(Long diaryId, String uid) throws AccessDeniedException {
        User user = userRepository.findByUid(uid);
        Optional<Diary> diary = diaryRepository.findById(diaryId);
        LocalDateTime res = null;
        if(diary.isPresent()){
            Diary d = diary.get();
            res = d.getCreatedAt();
            if(d.getUser().getUid().equals(user.getUid())){
                diaryRepository.delete(d);
                statisticsService.updateKeywordStatistics(uid,res.toLocalDate());
            }
            else{
                throw new AccessDeniedException("Diary is not owned by : " + uid);
            }
        }else{
            throw new IllegalArgumentException("Diary not found");
        }
        return res.toLocalDate().toString();
    }
    
    @Transactional
    public DiaryResponseDto updateDiary(DiaryUpdateDto updateDto,Long diaryId, String uid) throws AccessDeniedException {
        
        String date = deleteDiary(diaryId, uid);
        DiaryCreateDto createDto = new DiaryCreateDto();
        createDto.setDiaryDate(date);
        createDto.setRawDiary(updateDto.getRawDiary());
        return createDiary(createDto, uid);
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
    
    private User findUserByUid(String uid) {
        User user = userRepository.findByUid(uid);
        if (user == null) {
            throw new IllegalArgumentException("User not found with uid: " + uid);
        }
        return user;
    }
    
    private void validateDiaryDoesNotExistForDate(String uid, LocalDate diaryDate) {
        logger.info("validateDiaryDoesNotExistForDate");
        LocalDateTime startOfDay = diaryDate.atStartOfDay();
        LocalDateTime endOfDay = diaryDate.atTime(LocalTime.MAX);
        List<Diary> existingDiaries = diaryRepository.findByUser_UidAndCreatedAtBetween(uid, startOfDay, endOfDay);
        if (!existingDiaries.isEmpty()) {
            throw new DiaryAlreadyExistsException("이미 해당 날짜(" + diaryDate.toString() + ")에 작성된 일기가 존재합니다.");
        }
    }
    
    private double logAiProcessingTime(long startTimeAiNanos) {
        long endTimeAiNanos = System.nanoTime();
        long durationAiNanos = endTimeAiNanos - startTimeAiNanos;
        double secs = durationAiNanos / 1_000_000_000.0;
        System.out.println("AI processing finished in " + secs + " seconds");
        return secs;
    }
    
    
    private Diary createAndSaveInitialDiary(User user, String rawDiary, String rephrasedDiary, LocalDateTime createdAt, String labelEmotion,String sum) {
        Diary diary = Diary.builder()
                .user(user)
                .rawDiary(rawDiary)
                .rephrasedDiary(rephrasedDiary)
                .createdAt(createdAt)
                .summary(sum) // 요약은 나중에 채워질 수 있음
                .LabelEmotion(labelEmotion)
                .diaryKeywords(new ArrayList<>())
                .diaryEmotions(new ArrayList<>())
                .build();
        return diaryRepository.save(diary); // DiaryEmotion, DiaryKeyword 연결 전에 ID 확보 위해 먼저 저장
    }
    
    private void processAndLinkEmotions(Diary diary, Map<String, Double> aiEmotionScores) {
        if (aiEmotionScores == null) aiEmotionScores = Collections.emptyMap();
        
        // AI의 diaryEmotions명의 Set
        Set<String> incomingEmotionNames = new HashSet<>(aiEmotionScores.keySet());
        
        // DB에서 해당 감정 엔티티 미리 모두 조회
        List<Emotion> foundEmotions = emotionRepository.findByEmotionStrIn(incomingEmotionNames.stream().toList());
        //감정 이름과 Emotion객체 바인딩맵
        Map<String, Emotion> dbFoundEmotionMap = foundEmotions.stream()
                .collect(Collectors.toMap(Emotion::getEmotionStr, Function.identity()));
        
        // 신규 감정 엔티티 저장 필요시 saveAll
        List<Emotion> newEmotions = new ArrayList<>();
        for (String e : incomingEmotionNames) {
            if (!dbFoundEmotionMap.containsKey(e)) {
                Emotion newEm = new Emotion();
                newEm.setEmotionStr(e);
                newEmotions.add(newEm);
            }
        }
        if (!newEmotions.isEmpty()) {
            emotionRepository.saveAll(newEmotions);//emotion에 새로운 맵 emotion들 저장
            for (Emotion e : newEmotions) dbFoundEmotionMap.put(e.getEmotionStr(), e);
        }
        
        List<DiaryEmotion> currentDiaryEmotions = diary.getDiaryEmotions();
        aiEmotionScores.forEach((emotionStr,score) -> {
            Emotion emotion = dbFoundEmotionMap.get(emotionStr);
            DiaryEmotionId deid = DiaryEmotionId.builder().
                    did(diary.getId()).
                    eid(emotion.getId()).
                    build();
            DiaryEmotion addingEmotion = DiaryEmotion.builder().
                    id(deid).
                    diary(diary).
                    emotion(emotion).
                    score(score).
                    build();
            currentDiaryEmotions.add(addingEmotion);
        });
    }
    
    
    private void processAndLinkKeywords(Diary diary, List<aiKeywordDto> aiKeywordDtos) {
        if (aiKeywordDtos == null) aiKeywordDtos = Collections.emptyList();
        
        // AI 응답의 키워드: (text|label) -> aiKeywordDto
        Map<String, aiKeywordDto> aiKeywordsDtoMap = aiKeywordDtos.stream()
                .collect(Collectors.toMap(
                        dto -> dto.getText() + "|" + (dto.getLabel() == null ? "NULL_LABEL" : dto.getLabel()),
                        Function.identity()));
        
        // DB에서 text로 일괄 조회
        Set<String> uniqueTexts = aiKeywordDtos.stream().map(aiKeywordDto::getText).collect(Collectors.toSet());
        List<Keyword> existingKeywords = uniqueTexts.isEmpty() ? Collections.emptyList()
                : keywordRepository.findByKeywordStrIn(new ArrayList<>(uniqueTexts));
        Map<String, Keyword> dbKeywordEntityMap = existingKeywords.stream()
                .collect(Collectors.toMap(
                        kw -> kw.getKeywordStr() + "|" + (kw.getLabel() == null ? "NULL_LABEL" : kw.getLabel()),
                        Function.identity(),
                        (a, b) -> a));
        
        // 새로 추가해야 하는 Keyword 엔티티 생성
        List<Keyword> newKeywordsToSave = new ArrayList<>();
        for (String aiKey : aiKeywordsDtoMap.keySet()) {
            if (!dbKeywordEntityMap.containsKey(aiKey)) {
                aiKeywordDto dto = aiKeywordsDtoMap.get(aiKey);
                Keyword newKeyword = Keyword.builder()
                        .keywordStr(dto.getText())
                        .label(dto.getLabel())
                        .build();
                newKeywordsToSave.add(newKeyword);
            }
        }
        if (!newKeywordsToSave.isEmpty()) {
            keywordRepository.saveAll(newKeywordsToSave);
            for (Keyword k : newKeywordsToSave)
                dbKeywordEntityMap.put(k.getKeywordStr() + "|" + (k.getLabel() == null ? "NULL_LABEL" : k.getLabel()), k);
        }
        
        // 현재 DiaryKeyword ID Set
        Set<DiaryKeywordId> currentIds = diary.getDiaryKeywords().stream()
                .map(DiaryKeyword::getId).collect(Collectors.toSet());
        
        // AI 기반 차집합 비교 (DiaryKeywordId)
        Set<DiaryKeywordId> aiIds = aiKeywordDtos.stream().map(dto -> {
            Keyword k = dbKeywordEntityMap.get(dto.getText() + "|" + (dto.getLabel() == null ? "NULL_LABEL" : dto.getLabel()));
            return (k != null && diary.getId() != null) ? new DiaryKeywordId(diary.getId(), k.getId()) : null;
        }).filter(Objects::nonNull).collect(Collectors.toSet());
        
        // 삭제: 현재 있는데 AI에 없는 것
        diary.getDiaryKeywords().removeIf(dk -> !aiIds.contains(dk.getId()));
        
        // 추가: AI에 있는데 현재 없는 것
        for (aiKeywordDto dto : aiKeywordDtos) {
            Keyword keyword = dbKeywordEntityMap.get(dto.getText() + "|" + (dto.getLabel() == null ? "NULL_LABEL" : dto.getLabel()));
            if (keyword == null || diary.getId() == null) continue;
            DiaryKeywordId dkId = new DiaryKeywordId(diary.getId(), keyword.getId());
            if (!currentIds.contains(dkId)) {
                DiaryKeyword dk = DiaryKeyword.builder()
                        .id(dkId)
                        .diary(diary)
                        .keyword(keyword)
                        .build();
                diary.getDiaryKeywords().add(dk);
            }
        }
    }
    
    @Transactional
    public void resetStat(String uid){
        List<Diary> diaries = diaryRepository.findAll();
        for(Diary diary : diaries){
            statisticsService.updateKeywordStatistics(uid,diary.getCreatedAt().toLocalDate());
        }
    }
    
    @Transactional
    public void createPhotoDiary(String uid, DiaryCreateDto createDto, List<MultipartFile> photos){
        long overallStartTime = System.nanoTime();
        //유저 찾기
        User user = findUserByUid(uid);
        //날짜 파싱
        LocalDate diaryDate = LocalDate.parse(createDto.getDiaryDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        //해당 날짜 일기 없는지 찾기
        validateDiaryDoesNotExistForDate(uid, diaryDate);
        //AI 응답
        long aiStartTime = System.nanoTime();
        aiResponseDto aiResponse = fastApiService.callAnalyze(createDto.getRawDiary());
        double secs = logAiProcessingTime(aiStartTime);
        //정제된 일기 줄바꿈
        String rephrasedString = getResult(aiResponse);
        //save(diary)
        Diary diary = createAndSaveInitialDiary(user, createDto.getRawDiary(), rephrasedString, diaryDate.atTime(11,0), aiResponse.getEmotion().getLabel(),aiResponse.getSummary());
        processAndLinkPhotos(diary,aiResponse.getParagraphs());
        //Emotion 저장
        processAndLinkEmotions(diary, aiResponse.getEmotion().getScores());
        //Keyword 저장
        processAndLinkKeywords(diary, aiResponse.getKeywords());
        //주간 통계
        statisticsService.updateKeywordStatistics(uid, diaryDate);
        
        long endTime = System.nanoTime();
        double duration = (endTime - overallStartTime) / 1_000_000_000.0;
        double dur = duration - secs;
        logger.info("----------------------Diary creation time : [{}]-------------------",dur);
        return diary.toResponseDto();
    }
    
    private void processAndLinkPhotos(Diary diary, List<ParagraphDto> paragraphs) {
        List<Photo> photos = new ArrayList<>();
        
    }
    
}
