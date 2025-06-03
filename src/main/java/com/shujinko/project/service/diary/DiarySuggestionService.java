package com.shujinko.project.service.diary;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content; // Content 임포트 추가
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;     // Part 임포트 추가
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.shujinko.project.domain.dto.ai.GeminiSuggestion;
import com.shujinko.project.domain.dto.ai.SuggestionRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DiarySuggestionService {

    @Value("${gemini.project.id}")
    private String projectId;

    @Value("${gemini.location}")
    private String location;

    // 사용할 모델 이름 (Gemini 2.0 Flash-Lite 또는 1.5 Flash 등)
    // private static final String MODEL_NAME = "gemini-2.0-flash-lite-001"; // 혹은 "gemini-1.5-flash-001"
    private static final String MODEL_NAME = "gemini-2.0-flash-lite-001"; // 예시로 1.5 Flash 사용 (필요에 따라 변경)

    private VertexAI vertexAi;
    private GenerativeModel model;

    @PostConstruct
    public void init() {
        this.vertexAi = new VertexAI(projectId, location);

        // 수정된 시스템 프롬프트 내용 정의
        String systemPrompt = "당신은 사용자가 하루 동안 겪은 일들과 느낀 감정들을 좀 더 쉽고 풍부하게 일기에 담을 수 있도록 돕는 친절하고 공감 능력 높은 AI 가이드입니다. " +
                "사용자가 일기를 쓰다가 잠시 멈추면, 지금까지의 내용을 바탕으로 사용자의 경험, 생각, 감정을 더 깊이 탐색하고 구체적으로 표현할 수 있도록 부드럽게 질문하거나 다음 이야기의 실마리를 제공해주세요. " +
                "추천은 사용자가 하루를 되돌아보며 자연스럽게 떠올릴 수 있는 다양한 측면(예: 가장 인상 깊었던 순간, 특별히 강하게 느꼈던 감정, 새롭게 배우거나 깨달은 점, 고마웠던 사람이나 일, 조금 아쉬웠던 부분이나 이를 통해 배울 점 등)을 부드럽게 건드려주는 방식으로 이루어져야 합니다. " +
                "단순히 다음 사건을 나열하도록 유도하기보다는, 그 경험이 사용자에게 어떤 의미였는지, 어떤 감정을 불러일으켰는지, 무엇을 생각하게 했는지 등을 스스로 탐색하고 표현하도록 격려하는 질문이나 짧은 코멘트를 제공하는 것이 좋습니다. " +
                "목표는 사용자가 자신의 하루를 다각도로 살펴보고, 솔직한 마음을 편안하게 글로 풀어낼 수 있도록 돕는 것입니다.";

        Content systemInstruction = Content.newBuilder()
                .addParts(Part.newBuilder().setText(systemPrompt))
                .build();

        this.model = new GenerativeModel.Builder()
                .setModelName(MODEL_NAME) // 실제 사용할 모델명으로 설정
                .setVertexAi(this.vertexAi)
                .setSystemInstruction(systemInstruction)
                .build();

        System.out.println("VertexAI and GenerativeModel initialized for model: " + MODEL_NAME + " with enhanced system instruction for guided journaling.");
    }

    public GeminiSuggestion getDiarySuggestion(SuggestionRequest request) throws IOException {
        long startTime = System.currentTimeMillis();

        // 사용자 프롬프트: 시스템 지침은 모델 초기화 시 설정했으므로, 여기서는 실제 일기 내용과 최종 요청만 전달
        String userPromptText = String.format(
                "[사용자 일기 내용 시작]\n%s\n[사용자 일기 내용 끝]\n\n이 사용자가 다음에 이어서 쓸 만한 내용을 10글자 이내로 한 가지 추천해주세요." +
                        " 별도의 강조표시 같은것은 없어야합니다. 순수한 텍스트로만 추천을 해주세요.",
                request.getRawDiary()
        );

        GenerateContentResponse response = this.model.generateContent(userPromptText);

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