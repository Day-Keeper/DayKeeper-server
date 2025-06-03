package com.shujinko.project.service.diary;

import com.shujinko.project.domain.dto.diary.EmotionCountDto;
import com.shujinko.project.domain.dto.diary.KeywordCountDto;
import com.shujinko.project.domain.dto.diary.KeywordLabelKey;
import com.shujinko.project.domain.dto.diary.OneSentence;
import com.shujinko.project.domain.entity.diary.Diary;
import com.shujinko.project.domain.entity.diary.MonthlyKeywordStat;
import com.shujinko.project.domain.entity.diary.WeeklyKeywordStat;
import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.repository.diary.DiaryRepository;
import com.shujinko.project.repository.diary.MonthlyKeywordStatRepository;
import com.shujinko.project.repository.diary.WeeklyKeywordStatRepository;
import com.shujinko.project.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
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
    
    @Autowired
    public StatisticsService(DiaryRepository diaryRepository, WeeklyKeywordStatRepository weeklyKeywordStatRepository
    , UserRepository userRepository, MonthlyKeywordStatRepository monthlyKeywordStatRepository) {
        this.diaryRepository = diaryRepository;
        this.weeklyKeywordStatRepository = weeklyKeywordStatRepository;
        this.userRepository = userRepository;
        this.monthlyKeywordStatRepository = monthlyKeywordStatRepository;
    }
    
    public OneSentence oneSentenceKeyword(String uid, int year, int month, int weekNumber){
        Optional<User> opUser = userRepository.findById(uid);
        User user = null;
        if(opUser.isPresent()){
            user = opUser.get();
        }else{
            return new OneSentence("알맞은 유저가 없는데요");
        }
        
        
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        LocalDate firstSunday = firstDayOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate weekStart =  firstSunday.plusWeeks(weekNumber - 1);
        
        List<WeeklyKeywordStat> stats= weeklyKeywordStatRepository.findByUserAndWeekOfYear(user,weekStart);
        WeeklyKeywordStat randomStat = null;
        
        if (stats != null && !stats.isEmpty()) {
            Random random = new Random();
            int randomIndex = random.nextInt(stats.size());
            randomStat = stats.get(randomIndex);
        }else{
            return new OneSentence("이번주는 등장한 키워드가 없네요!");
        }
        return new OneSentence("이번주 에는 [" + randomStat.getKeywordStr()+"] 키워드가 ["+randomStat.getFrequency()+"]번 등장했네요!");
    }
    
    public List<KeywordCountDto> topWeekKeywords(String uid, int year, int month, int weekNumber){
        Optional<User> opUser = userRepository.findById(uid);
        User user = null;
        if(opUser.isPresent()){
            user = opUser.get();
        }else{
            return null;
        }
        
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1); //그 달의 첫날
        LocalDate firstSunday = firstDayOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate weekStart = firstSunday.plusWeeks(weekNumber - 1);
        logger.info("weekStart : {}\n", firstSunday);
        List<WeeklyKeywordStat> stats= weeklyKeywordStatRepository.findByUserAndWeekOfYear(user,weekStart);
        return stats.stream().map(WeeklyKeywordStat::toKeywordCountDto).toList();
    }
    
    public List<KeywordCountDto> topMonthKeywords(String uid, int year, int month){
        User user = userRepository.findByUid(uid);
        LocalDate[] startEndMonth = getMonthRange(year,month);
        LocalDateTime start = LocalDateTime.of(startEndMonth[0], LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(startEndMonth[1], LocalTime.MAX);
        return monthlyKeywordStatRepository.findByUserAndYearAndMonth(user,year,month).stream().map(MonthlyKeywordStat::toKeywordCountDto).toList();
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
    
    
    private List<EmotionCountDto> getTopEmotion(String uid, LocalDateTime start, LocalDateTime end) {
        List<Diary> diaryList = diaryRepository.findByUser_UidAndCreatedAtBetween(uid, start, end);
        
        System.out.println("-----------diaryList-------" + diaryList);
        System.out.println("Start : " + start + "\n End : " + end);
        
        Map<String, Long> frequencyMap = diaryList.stream()
                .map(Diary::getLabelEmotion)//각 diary에서 감정이름만 추출
                .filter(emotion -> emotion != null && !emotion.isEmpty())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        
        System.out.println(frequencyMap);
        
        return frequencyMap.entrySet().stream()
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
    
    @Async // 이 메서드를 비동기적으로 실행
    @Transactional // 이 메서드 자체를 하나의 트랜잭션으로 관리
    public void updateKeywordStatistics(String uid, LocalDate diaryDate) {
        Object lock = userLocks.computeIfAbsent(uid, k -> new Object());
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
                        .limit(5)
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
                        .limit(5)
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
                
            } catch (Exception e) {
                System.err.println(Thread.currentThread().getName() + ": 비동기 주간 키워드 통계 업데이트 중 오류 발생 for user " + uid + ", date " + diaryDate);
                e.printStackTrace();
            }
        }
    }
    
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
}
