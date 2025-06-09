package com.shujinko.project.service.diary;

import com.shujinko.project.domain.dto.diary.EmotionCountDto;
import com.shujinko.project.domain.dto.diary.KeywordCountDto;
import com.shujinko.project.domain.dto.diary.KeywordLabelKey;
import com.shujinko.project.domain.dto.diary.OneSentence;
import com.shujinko.project.domain.entity.diary.*;
import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.repository.diary.*;
import com.shujinko.project.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StatisticsService {
    
    private static final ConcurrentHashMap<String,Object> userLocks = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(StatisticsService.class);
    private final DiaryRepository diaryRepository;
    private final WeeklyKeywordStatRepository weeklyKeywordStatRepository;
    private final UserRepository userRepository;
    private final MonthlyKeywordStatRepository monthlyKeywordStatRepository;
    private final Day7KeywordStatRepository day7KeywordStatRepository;
    private final Day30KeywordStatRepository day30KeywordStatRepository;
    private final EmotionRepository emotionRepository;
    private final DiarySuggestionService diarySuggestionService;
    private final ReportRepository reportRepository;
    @Autowired
    public StatisticsService(DiaryRepository diaryRepository, WeeklyKeywordStatRepository weeklyKeywordStatRepository
    , UserRepository userRepository, MonthlyKeywordStatRepository monthlyKeywordStatRepository,
                             Day7KeywordStatRepository day7KeywordStatRepository,
                             Day30KeywordStatRepository day30KeywordStatRepository, EmotionRepository emotionRepository,
                             DiarySuggestionService diarySuggestionService, ReportRepository reportRepository) {
        this.diaryRepository = diaryRepository;
        this.weeklyKeywordStatRepository = weeklyKeywordStatRepository;
        this.userRepository = userRepository;
        this.monthlyKeywordStatRepository = monthlyKeywordStatRepository;
        this.day7KeywordStatRepository = day7KeywordStatRepository;
        this.day30KeywordStatRepository = day30KeywordStatRepository;
        this.emotionRepository = emotionRepository;
        this.diarySuggestionService = diarySuggestionService;
        this.reportRepository = reportRepository;
    }
    
    public OneSentence oneSentenceKeyword(String uid) throws Exception {
        Optional<User> opUser = userRepository.findById(uid);
        LocalDate localDate = LocalDate.now();
        int year = localDate.getYear();
        int month = localDate.getMonthValue();
        int week = localDate.get(WeekFields.of(DayOfWeek.SUNDAY, 1).weekOfMonth());
        LocalDate lastMonth = localDate.minusMonths(1);
        User user = null;
        if(opUser.isPresent()){
            user = opUser.get();
        }else{
            return new OneSentence("알맞은 유저가 없는데요");
        }
        
        Optional<Report> report7 = reportRepository.findByUserAndSentenceType(user, "DAY7");
        Optional<Report> report30 = reportRepository.findByUserAndSentenceType(user, "DAY30");
        List<Report> reportThisMonth = reportRepository.findByUserAndYearAndMonthAndSentenceType(user,year,month, "MONTH");
        List<Report> reportThisWeek = reportRepository.findByUserAndYearAndMonthAndSentenceTypeOrderByWeekOfMonthAsc(user, year, month,"WEEK");
        List<Report> reportLastMonth = reportRepository.findByUserAndYearAndMonthAndSentenceType(user, lastMonth.getYear(), lastMonth.getMonthValue(), "MONTH");
        List<Report> reportLastWeek = reportRepository.findByUserAndYearAndMonthAndSentenceTypeOrderByWeekOfMonthAsc(user, lastMonth.getYear(), lastMonth.getMonthValue(), "WEEK");
        
        List<Report> allReports = new ArrayList<>();
        
        report7.ifPresent(allReports::add); // Optional에서 값이 있으면 추가
        report30.ifPresent(allReports::add); // Optional에서 값이 있으면 추가
        
        allReports.addAll(reportThisMonth);
        allReports.addAll(reportThisWeek);
        allReports.addAll(reportLastMonth);
        allReports.addAll(reportLastWeek);
        
        if(allReports.isEmpty()){
            logger.info("oneSentenceKeyword : 유저 {}의 통계가 없습니다.", uid);
            return new OneSentence("이번주는 등장한 키워드가 없네요!");
        }
        Random random = new Random();
        int randomIndex = random.nextInt(allReports.size()); // 리스트 크기 내에서 랜덤 인덱스 생성
        Report randomReport = allReports.get(randomIndex); // 랜덤으로 선택된 Report
        String sentence = randomReport.getSentence();
        return new OneSentence(sentence);
    }
    
    /**
     * 미리계산한 주간 통계 리턴*/
    public List<KeywordCountDto> topWeekKeywords(String uid, int year, int month, int weekNumber){
        User user = getUser(uid);
        if (user == null) return null;
        
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1); //그 달의 첫날
        LocalDate firstSunday = firstDayOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate weekStart = firstSunday.plusWeeks(weekNumber - 1);
        logger.info("weekStart : {}\n", firstSunday);
        List<WeeklyKeywordStat> stats= weeklyKeywordStatRepository.findByUserAndWeekOfYear(user,weekStart);
        return stats.stream().map(WeeklyKeywordStat::toKeywordCountDto).toList();
    }
    
    
    
    /**
     * 미리계산한 월간 통계 리턴*/
    public List<KeywordCountDto> topMonthKeywords(String uid, int year, int month){
        User user = userRepository.findByUid(uid);
        LocalDate[] startEndMonth = getMonthRange(year,month);
        LocalDateTime start = LocalDateTime.of(startEndMonth[0], LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(startEndMonth[1], LocalTime.MAX);
        return monthlyKeywordStatRepository.findByUserAndYearAndMonth(user,year,month).stream().map(MonthlyKeywordStat::toKeywordCountDto).toList();
    }
    public List<KeywordCountDto> day7Keywords(String uid){
        User user = getUser(uid);
        return day7KeywordStatRepository.findByUser(user).stream().map(Day7KeywordStat::toKeywordCountDto).toList();
    }
    public List<KeywordCountDto> day30Keywords(String uid){
        User user = getUser(uid);
        return day30KeywordStatRepository.findByUser(user).stream().map(Day30KeywordStat::toKeywordCountDto).toList();
    }
    
    public List<EmotionCountDto> topWeekEmotion(String uid, int year, int month, int weekNumber){
        LocalDate[] startEndWeek = getWeekRange(year,month,weekNumber);
        LocalDateTime start = LocalDateTime.of(startEndWeek[0], LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(startEndWeek[1], LocalTime.MAX);
        return getTopEmotion(uid, start, end);
    }
    public List<EmotionCountDto> topMonthEmotion(String uid, int year, int month){
        LocalDate[] startEndMonth = getMonthRange(year,month);
        LocalDateTime start = LocalDateTime.of(startEndMonth[0], LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(startEndMonth[1], LocalTime.MAX);
        return getTopEmotion(uid, start, end);
    }
    public List<EmotionCountDto> day7Emotion(String uid){
        LocalDate now = LocalDate.now();
        LocalDate day7before = now.minusDays(7);
        LocalDateTime start = LocalDateTime.of(day7before, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(now, LocalTime.MAX);
        return getTopEmotion(uid, start, end);
    }
    public List<EmotionCountDto> day30Emotion(String uid){
        LocalDate now = LocalDate.now();
        LocalDate day30before = now.minusMonths(1);
        LocalDateTime start = LocalDateTime.of(day30before, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(now, LocalTime.MAX);
        return getTopEmotion(uid, start, end);
    }
    
    private List<EmotionCountDto> getTopEmotion(String uid, LocalDateTime start, LocalDateTime end) {
        List<Diary> diaryList = diaryRepository.findByUser_UidAndCreatedAtBetween(uid, start, end);
        List<Emotion> emotionList = emotionRepository.findAll();
        Map<String,Long> freqMap = emotionList.stream().collect(Collectors.toMap(Emotion::getEmotionStr,
                emotion -> 0L,
                (existingValue,newValue)->existingValue));
        Map<String, Long> frequencyMap = diaryList.stream()//감정들의 횟수 추가
                .map(Diary::getLabelEmotion)
                .filter(emotion -> emotion != null && !emotion.isEmpty())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        
        freqMap.putAll(frequencyMap);
        
        System.out.println("-----------diaryList-------" + diaryList);
        System.out.println("Start : " + start + "\n End : " + end);
        
        System.out.println(freqMap);
        
        return freqMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new EmotionCountDto(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
    
    
    /**
     * 특정 년/월/주의 [첫날/마지막날]의 날짜 리턴
     * */
    public static LocalDate[] getWeekRange(int year, int month, int weekNumber) {
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        
        // 이번 달 1일 기준, 그 주의 일요일 구하기
        int daysToPrevSunday = firstDayOfMonth.getDayOfWeek().getValue() % 7;
        LocalDate firstSunday = firstDayOfMonth.minusDays(daysToPrevSunday);
        
        //weekNumber가 1보다 작거나 같으면 첫 주 리턴
        if (weekNumber <= 0) {
            return new LocalDate[]{firstSunday, firstSunday.plusDays(6)};
        }
        
        // 계산된 주의 시작/끝
        LocalDate startOfWeek = firstSunday.plusWeeks(weekNumber - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        
        // 이번 달의 실제 시작일/마지막일
        LocalDate monthStart = firstDayOfMonth;
        LocalDate monthEnd = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());
        
        // 만약 계산된 주가 아예 이전 달이면 null 처리
        if (endOfWeek.isBefore(monthStart)) {
            return new LocalDate[]{null, null};
        }
        
        // 계산된 주가 이번 달을 아예 벗어나면, 마지막 주 리턴
        if (startOfWeek.isAfter(monthEnd)) {
            LocalDate lastSunday = firstSunday;
            while (lastSunday.plusDays(6).isBefore(monthEnd)) {
                lastSunday = lastSunday.plusWeeks(1);
            }
            return new LocalDate[]{lastSunday, lastSunday.plusDays(6)};
        }
        
        // 정상 범위
        return new LocalDate[]{startOfWeek, endOfWeek};
    }
    
    public static LocalDate[] getMonthRange(int year, int month) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        
        return new LocalDate[]{firstDay, lastDay};
    }
    
    
    //-----업데이트 함수--------------
    @Async // 이 메서드를 비동기적으로 실행
    @Transactional
    public void updateKeywordStatistics(String uid, LocalDate diaryDate) {
        Object lock = userLocks.computeIfAbsent(uid, k -> new Object());
        final int topN = 20;//몇개 뽑을건지 고름
        synchronized (lock) {
            try {
                User user = userRepository.findByUid(uid);
                if (user == null) {
                    System.err.println("비동기 통계 업데이트: 사용자 '" + uid + "'를 찾을 수 없습니다.");
                    return;
                }
                
                System.out.println(Thread.currentThread().getName() + ": 비동기 주간 키워드 통계 업데이트 시작 for User " + uid + ", Date " + diaryDate);
                
                int year = diaryDate.getYear();
                LocalDate weekStart = diaryDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));//
                
                LocalDate startOfWeekDate = diaryDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
                LocalDate endOfWeekDate = diaryDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
                LocalDateTime startOfWeekDateTime = startOfWeekDate.atStartOfDay();
                LocalDateTime endOfWeekDateTime = endOfWeekDate.atTime(LocalTime.MAX);
                
                Map<KeywordLabelKey, Long> weekKeywordCounts =  getKeywordCountMap(uid, startOfWeekDateTime, endOfWeekDateTime);
                
                weeklyKeywordStatRepository.deleteByUserAndWeekOfYear(user, weekStart);
                
                
                List<WeeklyKeywordStat> newWeeklyStats = weekKeywordCounts.entrySet().stream()
                        .sorted(Map.Entry.<KeywordLabelKey, Long>comparingByValue().reversed() // 빈도수 내림차순 정렬
                                .thenComparing(entry -> entry.getKey().keywordStr())
                                .thenComparing(entry -> {
                                    String lbl = entry.getKey().label();
                                    return lbl == null ? "" : lbl;
                                }))
                        .limit(topN)
                        .map(entry -> WeeklyKeywordStat.builder()
                                .user(user)
                                .weekOfYear(weekStart)
                                .keywordStr(entry.getKey().keywordStr())
                                .label(entry.getKey().label())
                                .frequency(entry.getValue())
                                .build())
                        .collect(Collectors.toList());
                
                if (!newWeeklyStats.isEmpty()) {
                    weeklyKeywordStatRepository.saveAll(newWeeklyStats);
                    System.out.println(Thread.currentThread().getName() + ": 비동기 주간 Top 5 키워드 통계 DB 저장 완료 (" + year + "-W" + weekStart + ") for user " + uid);
                } else {
                    System.out.println(Thread.currentThread().getName() + ": 비동기 주간 키워드 통계: 저장할 키워드 없음 (" + year + "-W" + weekStart + ") for user " + uid);
                }
                
                //----------------------------MONTH STAT-------------------
                int month = diaryDate.getMonthValue();
                LocalDate monthStart = LocalDate.of(year, month, 1);
                LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
                
                LocalDateTime startOfMonthDateTime = monthStart.atStartOfDay();
                LocalDateTime endOfMonthDateTime = monthEnd.atTime(LocalTime.MAX);
                logger.info("start Month : {} \n end Month : {}", startOfMonthDateTime, endOfMonthDateTime);
                
                Map<KeywordLabelKey, Long> monthKeywordCounts = getKeywordCountMap(uid, startOfMonthDateTime, endOfMonthDateTime);
                monthlyKeywordStatRepository.deleteByUserAndYearAndMonth(user, year, month);
                
                List<MonthlyKeywordStat> newMonthlyStats = monthKeywordCounts.entrySet().stream()
                        .sorted(Map.Entry.<KeywordLabelKey, Long>comparingByValue().reversed() // 빈도수 내림차순 정렬
                                .thenComparing(entry -> entry.getKey().keywordStr())
                                .thenComparing(entry -> {
                                    String lbl = entry.getKey().label();
                                    return lbl == null ? "" : lbl; // null 레이블 처리
                                }))
                        .limit(topN)
                        .map(entry -> MonthlyKeywordStat.builder()
                                .user(user)
                                .year(year)
                                .month(month)
                                .keywordStr(entry.getKey().keywordStr())
                                .label(entry.getKey().label())
                                .frequency(entry.getValue())
                                .build())
                        .collect(Collectors.toList());
                
                if (!newMonthlyStats.isEmpty()) {
                    monthlyKeywordStatRepository.saveAll(newMonthlyStats);
                    System.out.println(Thread.currentThread().getName() + ": 비동기 월간 Top 5 키워드 통계 DB 저장 완료 (" + year + "-W" + month + ") for user " + uid);
                } else {
                    System.out.println(Thread.currentThread().getName() + ": 비동기 월간 키워드 통계: 저장할 키워드 없음 (" + year + "-W" + month + ") for user " + uid);
                }
                
                updateWeekReport(user, diaryDate);
                updateMonthReport(user, diaryDate);
            } catch (Exception e) {
                System.err.println(Thread.currentThread().getName() + ": 비동기 주간 키워드 통계 업데이트 중 오류 발생 for user " + uid + ", date " + diaryDate);
                e.printStackTrace();
            }
        }
    }
    
    @Async
    @Transactional
    public void updateDay7KeywordStats(String uid, LocalDate diaryDate){
        Object lock = userLocks.computeIfAbsent(uid, k -> new Object());
        synchronized (lock){
            try {
                User user = userRepository.findById(uid).orElse(null);
                if(user == null){
                    logger.info("updateDay7KeywordStats : user {} 를 찾을 수 없음.",uid);
                    return;
                }
                logger.info("updateDay7KeywordStats : user {} 시작 ",uid);
                LocalDate now = LocalDate.now();
                LocalDate sevenDaysAgo = now.minusDays(7);
                if(diaryDate.isBefore(sevenDaysAgo)){//7일보다 전이면
                   logger.debug("updateDay7KeywordStats : user {} 7일 보다 전, 갱신안함",uid);
                   return;
                }
                LocalDateTime today = now.atTime(LocalTime.MAX);
                LocalDateTime day7Ago = sevenDaysAgo.atTime(LocalTime.MIN);
                Map<KeywordLabelKey, Long> day7Counts =  getKeywordCountMap(uid, day7Ago, today);
                
                day7KeywordStatRepository.deleteByUser(user);
                
                List<Day7KeywordStat> newDay7Stats = day7Counts.entrySet().stream()
                        .sorted(Map.Entry.<KeywordLabelKey, Long>comparingByValue().reversed() // 빈도수 내림차순 정렬
                                .thenComparing(entry -> entry.getKey().keywordStr())
                                .thenComparing(entry -> {
                                    String lbl = entry.getKey().label();
                                    return lbl == null ? "" : lbl;
                                }))
                        .limit(5)
                        .map(entry -> Day7KeywordStat.builder()
                                .user(user)
                                .keywordStr(entry.getKey().keywordStr())
                                .label(entry.getKey().label())
                                .frequency(entry.getValue())
                        .build())
                        .toList();
               
                if(!newDay7Stats.isEmpty()){
                    day7KeywordStatRepository.saveAll(newDay7Stats);
                    logger.info("updateDay7KeywordStats : user {} 성공",uid);
                }else{
                    logger.info("updateDay7KeywordStats : user {} 추가할 keyword 없음",uid);
                }
                updateDay7Report(user, diaryDate);
                logger.info("updateDay7KeywordStats : user {} 종료 ",uid);
            }catch(Exception e){
                logger.info("updateDay7KeywordStats : user {} error!",uid);
            }
        }
    }
    
    @Async
    @Transactional
    public void updateDay30KeywordStats(String uid, LocalDate diaryDate){
        Object lock = userLocks.computeIfAbsent(uid, k -> new Object());
        synchronized (lock){
            try {
                User user = userRepository.findById(uid).orElse(null);
                if(user == null){
                    logger.info("updateDay30KeywordStats : user {} 를 찾을 수 없음.",uid);
                    return;
                }
                logger.info("updateDay30KeywordStats : user {} 시작 ",uid);
                LocalDate now = LocalDate.now();
                LocalDate sevenDaysAgo = now.minusDays(30);
                if(diaryDate.isBefore(sevenDaysAgo)){//30일보다 전이면
                    logger.debug("updateDay30KeywordStats : user {} 30일 보다 전, 갱신안함",uid);
                    return;
                }
                LocalDateTime today = now.atTime(LocalTime.MAX);
                LocalDateTime day30Ago = sevenDaysAgo.atTime(LocalTime.MIN);
                Map<KeywordLabelKey, Long> day7Counts =  getKeywordCountMap(uid, day30Ago, today);
                
                day30KeywordStatRepository.deleteByUser(user);
                
                List<Day30KeywordStat> newDay30Stats = day7Counts.entrySet().stream()
                        .sorted(Map.Entry.<KeywordLabelKey, Long>comparingByValue().reversed() // 빈도수 내림차순 정렬
                                .thenComparing(entry -> entry.getKey().keywordStr())
                                .thenComparing(entry -> {
                                    String lbl = entry.getKey().label();
                                    return lbl == null ? "" : lbl;
                                }))
                        .limit(5)
                        .map(entry -> Day30KeywordStat.builder()
                                .user(user)
                                .keywordStr(entry.getKey().keywordStr())
                                .label(entry.getKey().label())
                                .frequency(entry.getValue())
                                .build())
                        .toList();
                if(!newDay30Stats.isEmpty()){
                    day30KeywordStatRepository.saveAll(newDay30Stats);
                    logger.info("updateDay30KeywordStats : user {} 성공",uid);
                }else{
                    logger.info("updateDay30KeywordStats : user {} 추가할 keyword 없음",uid);
                }
                updateDay30Report(user, diaryDate);
                logger.info("updateDay30KeywordStats : user {} 종료 ",uid);
            }catch(Exception e){
                logger.info("updateDay30KeywordStats : user {} error!",uid);
            }
        }
    }
    
    public void updateDay7Report(User user, LocalDate diaryDate) throws IOException{
        logger.info("updateDay7Report : user {} 시작 ",user.getUid());
        LocalDate sevenDaysAgo = diaryDate.minusDays(7);
        if(diaryDate.isBefore(sevenDaysAgo)){//7일보다 전이면
            logger.debug("updateDay7Report : user {} 7일 보다 전, 갱신안함",user.getUid());
            return;
        }
        List<EmotionCountDto> emotions = day7Emotion(user.getUid());
        List<KeywordCountDto> keywords = day7Keywords(user.getUid());
        Map<String,Long> keywordMap = keywords.stream()
                .collect(Collectors.toMap(KeywordCountDto::getKeyword, KeywordCountDto::getCount,
                        (existing, replacement) -> existing>replacement?existing:replacement)); // 중복 키 처리
        Map<String,Long> emotionMap = emotions.stream()
                .collect(Collectors.toMap(EmotionCountDto::getEmotion, EmotionCountDto::getCount,
                        (existing, replacement) -> existing>replacement?existing:replacement));
        KeywordEmotion keywordEmotion = new KeywordEmotion();
        keywordEmotion.setKeywords(keywordMap);
        keywordEmotion.setEmotions(emotionMap);
        
        OneSentence oneSentence = diarySuggestionService.getWeekOneSentence(keywordEmotion,"오늘 기준 7일 전");
        Optional<Report> optReport =
                reportRepository.findByUserAndSentenceType(user, "DAY7");
        if(optReport.isPresent()){
            Report report = optReport.get();
            report.setSentence(oneSentence.getSentence());
        }else{
            Report report = Report.builder()
                    .user(user)
                    .weekOfMonth(1) // DAY7는 항상 1주차로 간주
                    .sentenceType("DAY7")
                    .sentence(oneSentence.getSentence())
                    .build();
            reportRepository.save(report);
        }
    }
    
    public void updateDay30Report(User user, LocalDate diaryDate) throws IOException {
        logger.info("updateDay30Report : user {} 시작 ",user.getUid());
        LocalDate thirtyDaysAgo = diaryDate.minusDays(30);
        if(diaryDate.isBefore(thirtyDaysAgo)){//30일보다 전이면
            logger.debug("updateDay30Report : user {} 30일 보다 전, 갱신안함",user.getUid());
            return;
        }
        List<EmotionCountDto> emotions = day30Emotion(user.getUid());
        List<KeywordCountDto> keywords = day30Keywords(user.getUid());
        Map<String,Long> keywordMap = keywords.stream()
                .collect(Collectors.toMap(KeywordCountDto::getKeyword, KeywordCountDto::getCount,
                        (existing, replacement) -> existing>replacement?existing:replacement)); // 중복 키 처리
        Map<String,Long> emotionMap = emotions.stream()
                .collect(Collectors.toMap(EmotionCountDto::getEmotion, EmotionCountDto::getCount,
                        (existing, replacement) -> existing>replacement?existing:replacement));
        KeywordEmotion keywordEmotion = new KeywordEmotion();
        keywordEmotion.setKeywords(keywordMap);
        keywordEmotion.setEmotions(emotionMap);
        
        OneSentence oneSentence = diarySuggestionService.getWeekOneSentence(keywordEmotion,"오늘 기준 30일 전");
        Optional<Report> optReport =
                reportRepository.findByUserAndSentenceType(user, "DAY30");
        if(optReport.isPresent()){
            Report report = optReport.get();
            report.setSentence(oneSentence.getSentence());
        }else{
            Report report = Report.builder()
                    .user(user)
                    .weekOfMonth(1) // DAY30는 항상 1주차로 간주
                    .sentenceType("DAY30")
                    .sentence(oneSentence.getSentence())
                    .build();
            reportRepository.save(report);
        }
    }
    
    public void updateWeekReport(User user,LocalDate diaryDate) throws IOException {
        //TODO : 들어온 주의 1주일 sentence 작성
        logger.info("updateWeekReport : user {} 시작 ",user.getUid());
        WeekFields weekFields = WeekFields.of(DayOfWeek.SUNDAY, 1);
        int year = diaryDate.getYear();
        int month = diaryDate.getMonthValue();
        int weekNumber = diaryDate.get(weekFields.weekOfMonth());
        Optional<Report> optReport=
                reportRepository.findByUserAndYearAndMonthAndWeekOfMonthAndSentenceType(user,year,month,weekNumber,"WEEK");
        
        KeywordEmotion keywordEmotion = new KeywordEmotion();
        List<EmotionCountDto> emotions = topWeekEmotion(user.getUid(), year, month, weekNumber);
        List<KeywordCountDto> keywords = topWeekKeywords(user.getUid(), year, month, weekNumber);
        Map<String,Long> keywordMap = keywords.stream()
                .collect(Collectors.toMap(KeywordCountDto::getKeyword, KeywordCountDto::getCount,
                        (existing, replacement) -> existing>replacement?existing:replacement)); // 중복 키 처리
        Map<String,Long> emotionMap = emotions.stream()
                .collect(Collectors.toMap(EmotionCountDto::getEmotion, EmotionCountDto::getCount,
                        (existing, replacement) -> existing>replacement?existing:replacement));
        keywordEmotion.setKeywords(keywordMap);
        keywordEmotion.setEmotions(emotionMap);
        
        OneSentence oneSentence = diarySuggestionService.getWeekOneSentence(keywordEmotion,String.format("%d년 %d월 %d주차", year, month, weekNumber));
        if(optReport.isPresent()){
            Report report = optReport.get();
            report.setSentence(oneSentence.getSentence());
        }else {
            Report report = Report.builder()
                    .user(user)
                    .year(year)
                    .month(month)
                    .weekOfMonth(weekNumber)
                    .sentenceType("WEEK")
                    .sentence(oneSentence.getSentence())
                    .build();
            reportRepository.save(report);
        }
        logger.info("updateWeekReport : user {} 종료 ",user.getUid());
    }
    
    public void updateMonthReport(User user,LocalDate diaryDate) throws IOException {
        //TODO : 들어온 주의 1달 sentence 작성
        logger.info("updateMonthReport : user {} 시작 ",user.getUid());
        int year = diaryDate.getYear();
        int month = diaryDate.getMonthValue();
        int weekNumber = 1;
        Optional<Report> optReport=
                reportRepository.findByUserAndYearAndMonthAndWeekOfMonthAndSentenceType(user,year,month,weekNumber,"MONTH");
        
        KeywordEmotion keywordEmotion = new KeywordEmotion();
        List<EmotionCountDto> emotions = topMonthEmotion(user.getUid(), year, month);
        List<KeywordCountDto> keywords = topMonthKeywords(user.getUid(), year, month);
        Map<String,Long> keywordMap = keywords.stream()
                .collect(Collectors.toMap(KeywordCountDto::getKeyword, KeywordCountDto::getCount,
                        (existing, replacement) -> existing>replacement?existing:replacement)); // 중복 키 처리
        Map<String,Long> emotionMap = emotions.stream()
                .collect(Collectors.toMap(EmotionCountDto::getEmotion, EmotionCountDto::getCount,
                        (existing, replacement) -> existing>replacement?existing:replacement));
        keywordEmotion.setKeywords(keywordMap);
        keywordEmotion.setEmotions(emotionMap);
        
        OneSentence oneSentence = diarySuggestionService.getWeekOneSentence(keywordEmotion,String.format("%d년 %d월", year, month));
        if(optReport.isPresent()){
            Report report = optReport.get();
            report.setSentence(oneSentence.getSentence());
        }else {
            Report report = Report.builder()
                    .user(user)
                    .year(year)
                    .month(month)
                    .weekOfMonth(weekNumber)
                    .sentenceType("MONTH")
                    .sentence(oneSentence.getSentence())
                    .build();
            reportRepository.save(report);
        }
        logger.info("updateMonthReport : user {} 종료 ",user.getUid());
    }
    
    @Transactional
    @Async
    @Scheduled(cron = "0 0 0 * * *")
    public void updateAll(){
        List<User> users = userRepository.findAll();
        for (User user : users) {
            logger.info("Scheduled updateAll for user {} : 시작 ",user.getUid());
            updateDay30KeywordStats(user.getUid(), LocalDate.now());
            updateDay7KeywordStats(user.getUid(), LocalDate.now());
        }
    }
    
    
    
    //---------------유틸리티 함수----------
    private Map<KeywordLabelKey,Long> getKeywordCountMap(String uid, LocalDateTime startOfMonthDateTime, LocalDateTime endOfMonthDateTime) {
        List<Diary> diaries = diaryRepository.findByUser_UidAndCreatedAtBetween(uid, startOfMonthDateTime, endOfMonthDateTime);
        
        Map<KeywordLabelKey, Long> KeywordCounts = diaries.stream()
                .flatMap(d -> d.getDiaryKeywords().stream())             // 다이어리 키워드만 빼와서 스트림화
                .map(dk -> new KeywordLabelKey(                          //(keywordStr, label) 쌍으로 구성된 KeywordLabelKey 객체 생성
                        dk.getKeyword().getKeywordStr(),
                        dk.getKeyword().getLabel()                       // Keyword 엔티티의 label 가져오기
                ))
                .collect(Collectors.groupingBy(                          // 4. 그룹화 및 빈도수 계산
                        Function.identity(),                             //    KeywordLabelKey 객체 자체를 그룹화의 키로 사용
                        Collectors.counting()                            //    각 키(KeywordLabelKey)의 등장 횟수 계산
                ));
        return KeywordCounts;
    }
    private User getUser(String uid) {
        Optional<User> opUser = userRepository.findById(uid);
        User user = null;
        if(opUser.isPresent()){
            user = opUser.get();
        }else{
            return null;
        }
        return user;
    }
}
