package com.shujinko.project.service.diary;

import com.shujinko.project.domain.dto.diary.DiaryResponseDto;
import com.shujinko.project.domain.dto.diary.EmotionCountDto;
import com.shujinko.project.domain.entity.diary.Diary;
import com.shujinko.project.repository.diary.DiaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StatisticsService {
    
    private final DiaryRepository diaryRepository;
    
    @Autowired
    public StatisticsService(DiaryRepository diaryRepository) {
        this.diaryRepository = diaryRepository;
    }
    
    public List<EmotionCountDto> top3WeekEmotion(String uid, int year, int month, int weekNumber){
        LocalDate[] startEndWeek = getWeekRange(year,month,weekNumber);
        
        LocalDateTime start = LocalDateTime.of(startEndWeek[0], LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(startEndWeek[1], LocalTime.MAX);
        
        return getTop3Emotion(uid, start, end);
    }
    
    public List<EmotionCountDto> top3MothEmotion(String uid, int year, int month){
        LocalDate[] startEndMonth = getMonthRange(year,month);
        LocalDateTime start = LocalDateTime.of(startEndMonth[0], LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(startEndMonth[1], LocalTime.MAX);
        return getTop3Emotion(uid, start, end);
    }
    
    
    private List<EmotionCountDto> getTop3Emotion(String uid, LocalDateTime start, LocalDateTime end) {
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
                .limit(3)
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
    
    
    
}
