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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.security.Key;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
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
    private final WeeklyKeywordStatRepository weeklyKeywordStatRepository;
    
    @Autowired
    public DiaryService(DiaryRepository diaryRepository, UserRepository userRepository,
                        FastApiService fastApiService, EmotionRepository emotionRepository,
                        KeywordRepository keywordRepository, WeeklyKeywordStatRepository weeklyKeywordStatRepository) {
        this.diaryRepository = diaryRepository;
        this.userRepository = userRepository;
        this.fastApiService = fastApiService;
        this.emotionRepository = emotionRepository;
        this.keywordRepository = keywordRepository;
        this.weeklyKeywordStatRepository = weeklyKeywordStatRepository;
    }
    
    private record KeywordLabelKey(String keywordStr, String label){}
    
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
        updateWeeklyKeywordStatisticsAsync(uid, diaryDate);
        
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
                updateWeeklyKeywordStatisticsAsync(uid,res.toLocalDate());
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
    
    @Async // 이 메서드를 비동기적으로 실행
    @Transactional // 이 메서드 자체를 하나의 트랜잭션으로 관리
    public void updateWeeklyKeywordStatisticsAsync(String uid, LocalDate diaryDate) {
        try {
            // 비동기 작업이므로, 호출한 쪽의 트랜잭션과 분리됨.
            // User 객체를 uid로부터 다시 조회하는 것이 안전.
            User user = userRepository.findByUid(uid);
            if (user == null) {
                System.err.println("비동기 통계 업데이트: 사용자 '" + uid + "'를 찾을 수 없습니다.");
                return; // 또는 적절한 예외 로깅
            }
            
            System.out.println(Thread.currentThread().getName() + ": 비동기 주간 키워드 통계 업데이트 시작 for User " + uid + ", Date " + diaryDate);
            
            int year = diaryDate.getYear();
            int weekOfYear = diaryDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR); // ISO 8601 주차
            
            LocalDate startOfWeekDate = diaryDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
            LocalDate endOfWeekDate = diaryDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
            LocalDateTime startOfWeekDateTime = startOfWeekDate.atStartOfDay();
            LocalDateTime endOfWeekDateTime = endOfWeekDate.atTime(LocalTime.MAX);
            
            List<Diary> weeklyDiaries = diaryRepository.findByUser_UidAndCreatedAtBetween(uid, startOfWeekDateTime, endOfWeekDateTime);
            
            Map<KeywordLabelKey, Long> keywordCounts = weeklyDiaries.stream() // 1. 스트림 생성
                    .flatMap(d -> d.getDiaryKeywords().stream())             // 2. 모든 DiaryKeyword를 하나의 스트림으로 펼치기
                    .map(dk -> new KeywordLabelKey(                          // 3. (keywordStr, label) 쌍으로 구성된 KeywordLabelKey 객체 생성
                            dk.getKeyword().getKeywordStr(),
                            dk.getKeyword().getLabel()                       // Keyword 엔티티의 label 가져오기
                    ))
                    .collect(Collectors.groupingBy(                          // 4. 그룹화 및 빈도수 계산
                            Function.identity(),                             //    KeywordLabelKey 객체 자체를 그룹화의 키로 사용
                            Collectors.counting()                            //    각 키(KeywordLabelKey)의 등장 횟수 계산
                    ));
// ▲▲▲ 키워드 빈도수 계산 부분 수정 ▲▲▲
            
            weeklyKeywordStatRepository.deleteByUserAndYearAndWeekOfYear(user, year, weekOfYear);

// ▼▼▼ 새로운 Top 5 키워드 통계 생성 부분 수정 ▼▼▼
            List<WeeklyKeywordStat> newWeeklyStats = keywordCounts.entrySet().stream()
                    .sorted(Map.Entry.<KeywordLabelKey, Long>comparingByValue().reversed() // 빈도수 내림차순 정렬
                            // (선택) 빈도수가 같을 경우, 키워드 텍스트와 레이블로 추가 정렬하여 일관성 확보
                            .thenComparing(entry -> entry.getKey().keywordStr())
                            .thenComparing(entry -> {
                                String lbl = entry.getKey().label();
                                return lbl == null ? "" : lbl; // null 레이블 처리
                            }))
                    .limit(5)
                    .map(entry -> WeeklyKeywordStat.builder()
                            .user(user)
                            .year(year)
                            .weekOfYear(weekOfYear)
                            .keywordStr(entry.getKey().keywordStr()) // KeywordLabelKey에서 keywordStr 추출
                            .label(entry.getKey().label())         // KeywordLabelKey에서 label 추출 (WeeklyKeywordStat에 label 필드 추가 필요)
                            .frequency(entry.getValue())
                            .build())
                    .collect(Collectors.toList());
            
            if (!newWeeklyStats.isEmpty()) {
                weeklyKeywordStatRepository.saveAll(newWeeklyStats);
                System.out.println(Thread.currentThread().getName() + ": 비동기 주간 Top 5 키워드 통계 DB 저장 완료 (" + year + "-W" + weekOfYear + ") for user " + uid);
            } else {
                System.out.println(Thread.currentThread().getName() + ": 비동기 주간 키워드 통계: 저장할 키워드 없음 (" + year + "-W" + weekOfYear + ") for user " + uid);
            }
        } catch (Exception e) {
            // 비동기 작업 중 발생하는 예외는 호출자에게 직접 전달되지 않으므로, 여기서 처리해야 함.
            System.err.println(Thread.currentThread().getName() + ": 비동기 주간 키워드 통계 업데이트 중 오류 발생 for user " + uid + ", date " + diaryDate);
            e.printStackTrace(); // 실제 환경에서는 보다 정교한 에러 로깅 및 알림 처리 필요
        }
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
    
    private void logOverallProcessingTime(long overallStartTimeNanos, long aiProcessingDurationNanos) {
        long overallEndTimeNanos = System.nanoTime();
        long overallDurationNanos = overallEndTimeNanos - overallStartTimeNanos;
        // AI 처리 시간을 제외한 순수 Java 로직 처리 시간 계산 가능
        // long javaLogicDurationNanos = overallDurationNanos - aiProcessingDurationNanos;
        double overallSeconds = overallDurationNanos / 1_000_000_000.0;
        System.out.println("Total Diary creation (sync part) took: " + overallSeconds + " seconds");
    }
    
    
}
