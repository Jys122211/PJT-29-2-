package org.scoula.deposit.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.deposit.service.OcrService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;

    @PostMapping("/extract")
    public ResponseEntity<String> extractInfo(@RequestParam("file") MultipartFile file) {
        try {
            String result = ocrService.extractDepositInfo(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("OCR 처리 중 오류가 발생했습니다.");
        }
    }
}