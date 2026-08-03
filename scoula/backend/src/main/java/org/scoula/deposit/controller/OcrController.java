package org.scoula.deposit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.deposit.dto.OcrDepositResponseDTO;
import org.scoula.deposit.service.OcrService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
@Log4j2
public class OcrController {

    private final OcrService ocrService;

    @PostMapping("/extract")
    public ResponseEntity<?> extractInfo(@RequestParam("file") MultipartFile file) {
        try {
            OcrDepositResponseDTO result = ocrService.extractDepositInfo(file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, "INVALID_OCR_FILE", e.getMessage());
        } catch (ResourceAccessException e) {
            log.error("Gemini OCR 호출 시간 초과", e);
            return error(HttpStatus.GATEWAY_TIMEOUT, "OCR_TIMEOUT", "이미지 분석 시간이 초과되었습니다.");
        } catch (HttpStatusCodeException e) {
            log.error("Gemini OCR 호출 실패: status={}", e.getStatusCode(), e);
            return error(HttpStatus.BAD_GATEWAY, "OCR_PROVIDER_ERROR", "이미지 분석 서비스 호출에 실패했습니다.");
        } catch (IllegalStateException e) {
            log.error("Gemini OCR 응답 처리 실패", e);
            return error(HttpStatus.BAD_GATEWAY, "OCR_RESPONSE_ERROR", e.getMessage());
        } catch (Exception e) {
            log.error("OCR 처리 중 예상하지 못한 오류", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "OCR_FAILED", "OCR 처리 중 오류가 발생했습니다.");
        }
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String errorCode, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "FAILED");
        body.put("errorCode", errorCode);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
