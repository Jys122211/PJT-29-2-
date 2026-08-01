package org.scoula.deposit.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OcrService {

    // application.yml 등에 설정된 Gemini API Key
    @Value("${gemini.api.key}")
    private String apiKey;

    private final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    public String extractDepositInfo(MultipartFile file) throws Exception {
        // 1. 이미지를 Base64 문자열로 변환
        String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
        String mimeType = file.getContentType();

        // 2. Gemini API에 보낼 프롬프트 작성 (JSON 형태로만 반환하도록 강제)
        String prompt = "이 이미지는 은행 앱의 예금 상세 화면 캡처입니다. 다음 정보를 추출해서 반드시 JSON 형식으로만 반환해주세요. 다른 텍스트는 출력하지 마세요.\n" +
                "필요한 키: bankName (은행명), productName (상품명), principalAmount (가입금액, 숫자만), " +
                "joinDate (가입일, YYYYMMDD 형식), maturityDate (만기일, YYYYMMDD 형식), baseRate (기본금리, 숫자만).\n" +
                "읽을 수 없는 값은 null로 반환하세요.";

        // 3. 요청 JSON Body 구성 (Map 사용)
        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mime_type", mimeType);
        inlineData.put("data", base64Image);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("inline_data", inlineData);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(textPart, imagePart));

        requestBody.put("contents", List.of(content));

        // 4. API 호출
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(GEMINI_API_URL + apiKey, requestEntity, String.class);

        return response.getBody(); // 프론트엔드로 Gemini의 응답 결과를 그대로 전달
    }
}