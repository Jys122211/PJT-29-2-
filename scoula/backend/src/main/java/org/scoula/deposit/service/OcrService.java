package org.scoula.deposit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.scoula.deposit.dto.OcrDepositResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OcrService {

    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE
    );
    private static final String GEMINI_API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-3.6-flash}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    public OcrService() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(35_000);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public OcrDepositResponseDTO extractDepositInfo(MultipartFile file) throws IOException {
        validateFile(file);

        String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
        String mimeType = file.getContentType();

        String prompt = "은행 앱의 예금 상세 화면입니다. 화면에 실제로 표시된 값만 추출하세요. " +
                "계좌번호는 예금 상품에 연결된 계좌번호만 추출하고, 하이픈과 공백을 제거한 숫자 문자열로 반환하세요. " +
                "고객번호, 거래번호, 상품번호를 계좌번호로 잘못 추출하지 마세요. " +
                "금액은 쉼표와 원 단위를 제거한 정수, 날짜는 YYYYMMDD, 금리는 퍼센트 기호를 제거한 숫자로 반환하세요. " +
                "추측하지 말고 읽을 수 없는 값은 null로 반환하세요.";

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
        requestBody.put("generationConfig", createGenerationConfig());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        String apiUrl = String.format(GEMINI_API_URL_TEMPLATE, model);
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);

        return parseGeminiResponse(response.getBody());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일을 선택해 주세요.");
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("이미지는 10MB 이하만 업로드할 수 있습니다.");
        }

        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("PNG 또는 JPEG 이미지만 업로드할 수 있습니다.");
        }
    }

    private Map<String, Object> createGenerationConfig() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("bankName", nullableType("string", "은행명"));
        properties.put("productName", nullableType("string", "예금 상품명"));
        properties.put("accountNumber", nullableType(
                "string",
                "예금 상품 계좌번호. 하이픈과 공백을 제거한 숫자 문자열. 화면에서 확인할 수 없으면 null"
        ));
        properties.put("principalAmount", nullableType("integer", "가입 원금. 원 단위 정수"));
        properties.put("joinDate", nullableType("string", "가입일. YYYYMMDD 형식"));
        properties.put("maturityDate", nullableType("string", "만기일. YYYYMMDD 형식"));
        properties.put("baseRate", nullableType("number", "기본금리. 퍼센트 기호를 제외한 숫자"));
        properties.put("appliedRate", nullableType("number", "적용금리. 퍼센트 기호를 제외한 숫자"));

        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of(
                "bankName",
                "productName",
                "accountNumber",
                "principalAmount",
                "joinDate",
                "maturityDate",
                "baseRate",
                "appliedRate"
        ));
        schema.put("additionalProperties", false);

        Map<String, Object> textFormat = new HashMap<>();
        textFormat.put("mimeType", "APPLICATION_JSON");
        textFormat.put("schema", schema);

        Map<String, Object> responseFormat = new HashMap<>();
        responseFormat.put("text", textFormat);

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseFormat", responseFormat);
        return generationConfig;
    }

    private Map<String, Object> nullableType(String type, String description) {
        Map<String, Object> property = new HashMap<>();
        property.put("type", List.of(type, "null"));
        property.put("description", description);
        return property;
    }

    private OcrDepositResponseDTO parseGeminiResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("Gemini OCR 응답이 비어 있습니다.");
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");

            if (!candidates.isArray() || candidates.size() == 0) {
                throw new IllegalStateException("Gemini가 OCR 결과를 반환하지 않았습니다.");
            }

            JsonNode textNode = candidates.get(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            if (!textNode.isTextual() || textNode.asText().isBlank()) {
                throw new IllegalStateException("Gemini OCR 결과에 텍스트가 없습니다.");
            }

            return objectMapper.readValue(textNode.asText(), OcrDepositResponseDTO.class);
        } catch (IOException e) {
            throw new IllegalStateException("Gemini OCR 결과를 해석하지 못했습니다.", e);
        }
    }
}
