package com.shujinko.project.service.diary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content; // Content 임포트 추가
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;     // Part 임포트 추가
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.shujinko.project.config.JacksonConfig;
import com.shujinko.project.domain.dto.ai.GeminiSuggestion;
import com.shujinko.project.domain.dto.ai.SuggestionRequest;
import com.shujinko.project.domain.dto.diary.OneSentence;
import com.shujinko.project.domain.entity.diary.KeywordEmotion;
import com.shujinko.project.domain.entity.user.User;
import com.shujinko.project.repository.user.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class DiarySuggestionService {
    
    private final ObjectMapper objMapper;
    private final UserRepository userRepository;
    
    public DiarySuggestionService(ObjectMapper objMapper, UserRepository userRepository) {
        this.objMapper = objMapper;
        this.userRepository = userRepository;
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

    // 사용할 모델 이름 (Gemini 2.0 Flash-Lite 또는 1.5 Flash 등)
    // private static final String MODEL_NAME = "gemini-2.0-flash-lite-001"; // 혹은 "gemini-1.5-flash-001"
    private static final String MODEL_NAME = "gemini-2.0-flash-001"; // 예시로 1.5 Flash 사용 (필요에 따라 변경)
    private VertexAI vertexAi;
    private final Map<GeminiModels, GenerativeModel> models = new HashMap<>();

    @PostConstruct
    public void init() {
        this.vertexAi = new VertexAI(projectId, location);

        // 수정된 시스템 프롬프트 내용 정의
        String suggestionSysPrompt =
                """
                        너는 사용자의 일기 작성을 돕는 섬세하고 통찰력 있는 AI 에이전트야.
                        지금 사용자는 일기의 다음 내용으로 어떤 이야기를 적을지 고민하는 상황에 있어.
                        너의 목표는 아래 주어지는 사용자의 일기을 바탕으로, 그날의 경험과 감정을 더 깊이 탐색하고 하루의 경험을 더 풍부하고 생생하게 표현하도록 영감을 주는 질문이나 문장 제안을 하는 거야.
                        두번째 너의 핵심 역할은 사용자가 묘사한 특정 사건이나 감정의 파도가 지나간 **'그 다음'**으로 시점을 옮겨주는 것이다. 사용자의 글이 멈춘 지점에서, 그 행동의 **결과**나 그로 인해 파생된 **감정의 여운**에 대해 질문해야 한다. 사용자가 스스로 "아, 그 다음엔 어땠지?"라고 생각하게 만들어.
                        
                        
                        **RULES:**
-Output text only.  (No emojis, No markdown, No images)
-Always respond in Korean.
-Match the user's writing style and emotional tone.
-Always provide the suggestion in a single, short, and concise sentence.(under 25 words are best).
-Do not use unnecessary decorative or flowery language. The suggestion must always be clear and direct.
-The most important principle: Never ask again about facts the user has already mentioned or explained. This is a disrespectful action that ignores the user's words. You must respect what the user says and accept their words as fact.
-Acknowledge and summarize the reasons or emotions the user has explained, and then naturally move on to the next stage of emotion or situation.
-Be careful not to fixate on a specific person, event, or emotion. Broaden the perspective by looking at the entire diary entry, switching the topic, or asking about other surrounding situations.
-Be careful to not deep dive into user's emotion and thinking, read the whole diary and just give a light touch to those emotion.
-Instead of using a command tone like "Try doing~," gently guide the user with a declarative or interrogative sentence.
                **Exceptons:**
-if user did not provide any diary content, give him a silly and fun suggestion. Be playful and humorous and Create a light-hearted atmosphere.
                    **BAD EXAMPLE:**
                        
                        """;
        
        Content suggestionSysInstruction = Content.newBuilder()
                .addParts(Part.newBuilder().setText(suggestionSysPrompt))
                .build();

        models.put(GeminiModels.SUGGESTION,new GenerativeModel.Builder()
                .setModelName(MODEL_NAME) // 실제 사용할 모델명으로 설정
                .setVertexAi(this.vertexAi)
                .setSystemInstruction(suggestionSysInstruction)
                .build());
        
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
새로운 프로젝트의 마감일이 다가오면서, 한 주 내내 불안한 마음이 가장 컸군요.
일과 생각의 연결을 잠시 끊어낼 수 있게, 점심시간에 5분만이라도 가만히 창밖을 바라보는 건 어떤가요?
- 부가 설명 : 이런 부정적인 감정이면 대처를 조언해주거나 위로해줘.

### 예시 2
- 입력: { "emotions": {"성취감": 4, "뿌듯함": 2}, "keywords": {"운동": 5, "아침 루틴": 3} }
- 출력:
[지난 1주일, 지난 한달, n월m번째 주, n월]은 몸은 고단했어도 꾸준한 운동으로 성취감을 느끼며 스스로를 다잡은 한 주였네요.
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

    public GeminiSuggestion getDiarySuggestion(String uid, SuggestionRequest request) throws IOException {
        long startTime = System.currentTimeMillis();
        User user = userRepository.findByUid(uid);
        LocalDate birth = user.getBirthday();
        LocalDate now = LocalDate.now();
        int age = java.time.Period.between(birth, now).getYears();
        String ageStr = String.valueOf(age);
        
        // 사용자 프롬프트: 시스템 지침은 모델 초기화 시 설정했으므로, 여기서는 실제 일기 내용과 최종 요청만 전달
        String userPromptText = String.format(
                "[Diary Start]\n%s\n[Diary End] %s의 나이에 맞는 어투로 추천해줘.\n\n",
                request.getRawDiary(),ageStr
        );

        GenerateContentResponse response = models.get(GeminiModels.SUGGESTION).generateContent(userPromptText);

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        double elapsedTimeInSeconds = elapsedTime / 1000.0;
        System.out.println("Gemini response time for prompt: " + elapsedTimeInSeconds + " seconds");

        if (response.getCandidatesCount() > 0 && response.getCandidates(0).getContent().getPartsCount() > 0) {
            String suggestionText = response.getCandidates(0).getContent().getParts(0).getText();
            // Gemini가 시스템 프롬프트의 일부를 반복하거나 불필요한 안내 문구를 포함할 경우, 후처리 필요할 수 있음
            // 예: "다음 내용을 추천합니다: [실제 추천]" 과 같은 경우 -> "[실제 추천]" 부분만 추출
            return new GeminiSuggestion(suggestionText.trim()); // 간단한 trim 처리
        } else {
            System.err.println("No candidates found in response: " + response);
            // 사용자에게 보여줄 기본 메시지는 여기서 관리하거나, 예외를 발생시켜 상위에서 처리할 수 있습니다.
            return new GeminiSuggestion("아이디어를 생성하지 못했어요. 다시 시도해주세요.");
        }
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
}