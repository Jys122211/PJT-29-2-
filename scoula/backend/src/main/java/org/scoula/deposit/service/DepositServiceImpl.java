package org.scoula.deposit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.deposit.domain.UserDepositVO;
import org.scoula.deposit.dto.DepositDTO;
import org.scoula.deposit.dto.DepositListDTO;
import org.scoula.deposit.dto.DepositRequestDTO;
import org.scoula.deposit.exception.DepositNotFoundException;
import org.scoula.deposit.mapper.DepositMapper;
import org.scoula.deposit.mapper.DepositMapper;
import org.scoula.deposit.mapper.DepositHistoryMapper;
import org.scoula.deposit.util.LoginUser;
import org.scoula.deposit.util.AccountCrypto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class DepositServiceImpl implements DepositService {

    private final DepositMapper mapper;
    private final DepositHistoryMapper historyMapper;

    @Override
    public DepositListDTO getList() {
        Long userId = LoginUser.getUserId();

        List<UserDepositVO> list = mapper.getList(userId);

        list.forEach(vo -> vo.setAccountNumber(
                AccountCrypto.decrypt(vo.getAccountNumber())));

        List<DepositDTO> deposits = list.stream()
                .map(DepositDTO::of)
                .collect(Collectors.toList());

        long totalPrincipal = list.stream()
                .mapToLong(UserDepositVO::getPrincipalAmount)
                .sum();

        return new DepositListDTO(deposits.size(), totalPrincipal, deposits);
    }

    @Override
    public int getCount() {
        return mapper.getCount(LoginUser.getUserId());
    }

    @Override
    public DepositDTO get(Long userDepositId) {
        UserDepositVO vo = mapper.get(userDepositId, LoginUser.getUserId());

        if (vo == null) {
            throw new DepositNotFoundException();
        }

        vo.setAccountNumber(AccountCrypto.decrypt(vo.getAccountNumber()));

        return DepositDTO.of(vo);
    }

    @Transactional
    @Override
    public Long create(DepositRequestDTO request) {
        request.validate();

        Long userId = LoginUser.getUserId();

        UserDepositVO vo = request.toVO();
        vo.setAccountNumber(AccountCrypto.encrypt(vo.getAccountNumber()));
        vo.setGlobalId(UUID.randomUUID().toString());   // 글로벌 ID는 서버에서 생성
        vo.setUserId(userId);
        vo.setCreatedBy(userId);

        mapper.create(vo);

        // INSERT 뒤에 기록한다 — 그 전에는 userDepositId 가 없다
        historyMapper.insertSnapshot(vo.getUserDepositId(), "I", userId);

        log.info("예금 등록 완료 : userDepositId={}", vo.getUserDepositId());

        return vo.getUserDepositId();
    }

    @Transactional
    @Override
    public void update(Long userDepositId, DepositRequestDTO request) {
        request.validate();

        Long userId = LoginUser.getUserId();

        UserDepositVO vo = request.toVO();
        vo.setAccountNumber(AccountCrypto.encrypt(vo.getAccountNumber()));
        vo.setUserDepositId(userDepositId);
        vo.setUserId(userId);
        vo.setUpdatedBy(userId);

        // 바뀌기 전 상태를 먼저 남긴다
        historyMapper.insertSnapshot(userDepositId, "U", userId);

        // 0건이면 대상이 없거나 다른 사용자의 예금
        if (mapper.update(vo) == 0) {
            throw new DepositNotFoundException();
        }
        log.info("예금 수정 완료 : userDepositId={}", userDepositId);
    }

    @Transactional
    @Override
    public void delete(Long userDepositId) {
        Long userId = LoginUser.getUserId();

        // 지워지기 전 상태를 먼저 남긴다
        historyMapper.insertSnapshot(userDepositId, "D", userId);

        if (mapper.softDelete(userDepositId, userId) == 0) {
            throw new DepositNotFoundException();
        }
        log.info("예금 삭제 완료 : userDepositId={}", userDepositId);
    }
}
