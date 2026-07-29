package org.scoula.profitLoss.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.profitLoss.dto.UserDepositDTO;
import org.scoula.profitLoss.service.ProfitLossService;
import org.scoula.security.account.domain.CustomUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Log4j2
public class ProfitLossController {
    private final ProfitLossService service;

    @GetMapping("/deposits/list")
    public ResponseEntity<List<UserDepositDTO>> getDeposits(Authentication authentication) {
        CustomUser authenticatedUser = (CustomUser) authentication.getPrincipal();
        Long userId = authenticatedUser.getMember().getUserId();
        return ResponseEntity.ok(service.getDeposits(userId));
    }
}
