package com.shujinko.project.service.diary;

import com.shujinko.project.domain.entity.diary.Diary;
import com.shujinko.project.repository.diary.DiaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {
    
    private DiaryRepository diaryRepository;
    
    @Autowired
    public StatisticsService(DiaryRepository diaryRepository) {
        this.diaryRepository = diaryRepository;
    }
    
    public Map<String,Float> emotionStatistics(String uid,int year, int month, int weekNumber){
        
        LocalDate[] startEndWeek = getWeekRange(year,month,weekNumber);
        
        LocalDateTime start = LocalDateTime.of(startEndWeek[0], LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(startEndWeek[1], LocalTime.MAX);
        
        List<Diary> diaryList = diaryRepository.findByUser_UidAndCreatedAtBetween(uid,start,end);
        
        return null;
    }
    
    public static LocalDate[] getWeekRange(int year,int month, int weekNumber){
        LocalDate firstDay = LocalDate.of(year, month, 1);
        // <editor-fold desc="">
        DayOfWeek firstDayOfWeek = firstDay.getDayOfWeek();//목 == 0
        int daysToPrevSunday = (firstDayOfWeek.getValue()%7);
        LocalDate firstSunday = firstDay.minusDays(daysToPrevSunday);//2025-05-01(목) 의 4일전 = 2025-04-27
        
        // <editor-fold desc="calculate week">
        LocalDate startOfWeek = firstSunday.plusWeeks(weekNumber-1);//weekNumber-1번째 week의 일요일
        LocalDate endOfWeek = firstSunday.plusDays(6);//weekNumber-1번째 week의 첫날 + 6, 그 주의 토요일
        // </editor-fold>
        
        LocalDate monthStart = firstDay.withDayOfMonth(1);
        LocalDate monthEnd = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        
        if(endOfWeek.isBefore(monthStart)){
            return new LocalDate[]{firstSunday,firstSunday.plusDays(6)};
        }
        if(startOfWeek.isAfter(monthEnd)){
            LocalDate lastSunday = firstSunday;
            while(lastSunday.plusDays(6).isBefore(monthEnd)) {
                lastSunday = lastSunday.plusWeeks(1);
            }
        }
        
        
        return new LocalDate[]{startOfWeek, endOfWeek};
        // </editor-fold>
    }
}
