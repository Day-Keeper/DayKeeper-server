package com.shujinko.project.service.diary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content; // Content 임포트 추가
import com.google.cloud.vertexai.api.Part;     // Part 임포트 추가
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.shujinko.project.domain.dto.ai.GeminiSuggestion;
import com.shujinko.project.domain.dto.ai.SuggestionReqToGemini;
import com.shujinko.project.domain.dto.ai.SuggestionRequest;
import com.shujinko.project.domain.dto.diary.OneSentence;
import com.shujinko.project.domain.entity.diary.KeywordEmotion;
import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.repository.user.UserRepository;
import com.shujinko.project.service.user.GoogleCalendarService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class DiarySuggestionService {
    
    private final static String suggestionUrl = "http://127.0.0.1:8000";
    private final WebClient webClient;
    private final ObjectMapper objMapper;
    private final UserRepository userRepository;
    private final GoogleCalendarService googleCalendarService;
    
    public DiarySuggestionService(WebClient.Builder webClientBuilder, ObjectMapper objMapper, UserRepository userRepository, GoogleCalendarService googleCalendarService) {
        this.webClient = webClientBuilder.baseUrl(suggestionUrl).build();
        this.objMapper = objMapper;
        this.userRepository = userRepository;
        this.googleCalendarService = googleCalendarService;
    }
    
    public enum GeminiModels{
        SUGGESTION,
        ONE_SENTENCE_WEEK,
        ONE_SENTENCE_MONTH,
    }
    
    @Value("${gemini.project.id}")
    private String projectId;

    @Value("${gemini.location}")
    private String location;
    
    private Logger logger = LoggerFactory.getLogger(DiarySuggestionService.class);

    private static final String MODEL_NAME = "gemini-2.0-flash-lite";

    private VertexAI vertexAi;
    private final Map<GeminiModels, GenerativeModel> models = new HashMap<>();

    @PostConstruct
    public void init() {
        this.vertexAi = new VertexAI(projectId, location);
        
        String weekSentenceSysPrompt =  """
[역할(Role)]
당신은 사용자의 [다음 기간의 (키워드,감정)들 {오늘 기준 7일전, 오늘 기준 30일전까지, m번째달 n번째주, m번째 달}]을 통찰하고 그 기간을 가볍게 요약해주고 칭찬하거나 위로하며, 그에 맞는 가볍고 실천적인 조언을 건네는 현명한 조력자입니다.

[핵심 임무(Core Mission)]
1.  **핵심 테마 선정:** 입력된 여러 감정과 키워드 중에서, 가장 빈도가 높고 서로 연관성이 깊은 **핵심 테마(dominant theme)를 단 하나만** 찾아냅니다. (예: '불안' 감정과 '프로젝트', '마감일' 키워드 조합)

2.  **타겟화된 답변 생성:** **오직 이 핵심 테마에 대해서만**, 두 부분으로 구성된 간결한 답변을 생성합니다.
    - **파트 1: 핵심 통찰 (1~2 문장):** 선정된 테마에 대한 감정 상태를 따뜻하게 짚어줍니다.
    - **파트 2: 가벼운 대처 방안 (1개):** 그 테마에 맞는, 간단하고 실천하기 쉬운 대처 방안을 단 하나만 제안합니다.

[입력 데이터 형식(Input Data Format)]
사용자의 일주일치 데이터가 감정과 키워드의 빈도수를 포함한 JSON 형식으로 제공됩니다.
{
  "emotions": { "감정1": 빈도수, "감정2": 빈도수, ... },
  "keywords": { "키워드1": 빈도수, "키워드2": 빈도수, ... }
}

[출력 규칙 (Output Rules)]
- 모든 감정과 키워드를 종합하여 일반적인 요약을 하지 말고, **선택된 핵심 테마에만 집중**하세요.
- 답변은 '핵심 통찰'과 '가벼운 대처 방안' 두 부분으로 명확히 나누어 출력하세요.
- 각 부분은 매우 짧고 간결해야 합니다. 전체 답변이 4~5줄을 넘지 않도록 조절하세요.
- 대처 방안은 '숙제'처럼 느껴지지 않도록 가볍고 부드러운 톤으로 제안하세요.
- Markdown이나 다른 서식은 사용하지 마세요. 순수 텍스트로만 응답하세요.
- 반드시 존댓말을 하세요.

[예시 (Examples)]
### 예시 1: 여러 테마 중 가장 한가지 테마를 선택
- 입력:
{
  "emotions": {"불안": 5, "성취감": 2, "지침": 3},
  "keywords": {"프로젝트": 6, "마감일": 4, "운동": 3, "야근": 3}
}
- 설명: 이 경우, '성취감/운동' 테마와 '불안/프로젝트/마감일' 테마가 있음. 너는 이런 선택지들 중 하나를 랜덤하게 골라서 출력을 만들어 줘야해.
- 출력:
[새로운 프로젝트의 마감일이 다가오면서, 한 주 내내 불안한 마음이 가장 컸군요.
일과 생각의 연결을 잠시 끊어낼 수 있게, 점심시간에 5분만이라도 가만히 창밖을 바라보는 건 어떤가요?
- 부가 설명 : 이런 부정적인 감정이면 대처를 조언해주거나 위로해줘.

### 예시 2
- 입력: { "emotions": {"성취감": 4, "뿌듯함": 2}, "keywords": {"운동": 5, "아침 루틴": 3} }
- 출력:
[n월m번째 주, n월] 은 몸은 고단했어도 꾸준한 운동으로 성취감을 느끼며 스스로를 다잡은 한 주였네요.
이번 주말, 작은 성취를 이뤄낸 스스로에게 좋아하는 음료 한 잔을 선물해주는건 어떨까요?
- 부가 설명 : 이런 긍정적인 감정과 키워드는 더 기분좋은 하루를 보낼 수 있게 부추겨주는 말을 적어줘.
""";
        Content weekSentenceSysInstruction = Content.newBuilder()
                .addParts(Part.newBuilder().setText(weekSentenceSysPrompt))
                .build();
        
        models.put(GeminiModels.ONE_SENTENCE_WEEK,new GenerativeModel.Builder()
                .setModelName(MODEL_NAME) // 실제 사용할 모델명으로 설정
                .setVertexAi(this.vertexAi)
                .setSystemInstruction(weekSentenceSysInstruction)
                .build());
    }

    public GeminiSuggestion getDiarySuggestion(String uid, SuggestionRequest request) throws GeneralSecurityException, IOException,Exception {
        long startTime = System.currentTimeMillis();
        User user = userRepository.findByUid(uid);
        LocalDate birth = user.getBirthday();
        ZoneId seoulZoneId = ZoneId.of("Asia/Seoul");
        LocalDate now = LocalDate.now(seoulZoneId);
        int age = java.time.Period.between(birth, now).getYears();
        LocalDate diaryDate = LocalDate.parse(request.getDiaryDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        CompletableFuture<Map<LocalDate, List<String>>> userEvents = googleCalendarService.getCalendarEvents(uid);
        logger.info("Fetching events for user: {}, date {}  : {}", uid, diaryDate, userEvents.get().get(diaryDate));
        List<String> eventsForGivenDate = userEvents.get().getOrDefault(diaryDate, Collections.emptyList());
        String eventsJsonArrayString;
        if (eventsForGivenDate.isEmpty()) {
            eventsJsonArrayString = "[]"; // 이벤트가 없는 경우 빈 JSON 배열
        } else {
            eventsJsonArrayString = objMapper.writeValueAsString(eventsForGivenDate);
        }
        SuggestionReqToGemini suggesToGemini = SuggestionReqToGemini.builder().raw_diary(request.getRawDiary()).schedules(eventsJsonArrayString).age(age).build();
        GeminiSuggestion response = getSuggestion(suggesToGemini);
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        double elapsedTimeInSeconds = elapsedTime / 1000.0;
        System.out.println("Gemini response time for prompt: " + elapsedTimeInSeconds + " seconds");
        return response;
    }
    
    public OneSentence getWeekOneSentence(KeywordEmotion keywordEmotion,String range) throws IOException {
        try{
            String jsonData = objMapper.writerWithDefaultPrettyPrinter().writeValueAsString(keywordEmotion);
            
            String userPrompt = String.format("""
                    [%s] 기간의 감정 및 키워드 데이터야. 이걸 분석해서 조언해줘.
                    ```json
                    %s
                    ```
                    """, range,jsonData);
            var response = models.get(GeminiModels.ONE_SENTENCE_WEEK).generateContent(userPrompt);
            
            return new OneSentence(ResponseHandler.getText(response));
        }catch(IOException e){
            logger.error("getWeekOneSentence error ", e);
            return new OneSentence(null);
        }
    }
    
    
    @PreDestroy
    public void destroy() {
        if (this.vertexAi != null) {
            try {
                this.vertexAi.close();
                System.out.println("VertexAI client closed.");
            } catch (Exception e) {
                System.err.println("Error closing VertexAI client: " + e.getMessage());
            }
        }
    }
    
    
    public GeminiSuggestion getSuggestion(SuggestionReqToGemini request) {
        return webClient.post()
                .uri("/suggestion")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiSuggestion.class)
                .block();
    }
}