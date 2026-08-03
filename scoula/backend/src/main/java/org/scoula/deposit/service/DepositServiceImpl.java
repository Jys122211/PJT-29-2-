package org.scoula.deposit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.deposit.domain.UserDepositVO;
import org.scoula.deposit.dto.DepositDTO;
import org.scoula.deposit.dto.DepositListDTO;
import org.scoula.deposit.dto.DepositRequestDTO;
import org.scoula.deposit.exception.DepositNotFoundException;
import org.scoula.deposit.mapper.DepositMapper;
import org.scoula.deposit.util.LoginUser;
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

    @Override
    public DepositListDTO getList() {
        Long userId = LoginUser.getUserId();

        List<UserDepositVO> list = mapper.getList(userId);

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
        return DepositDTO.of(vo);
    }

    @Transactional
    @Override
    public Long create(DepositRequestDTO request) {
        request.validate();

        Long userId = LoginUser.getUserId();

        UserDepositVO vo = request.toVO();
        vo.setGlobalId(UUID.randomUUID().toString());   // 글로벌 ID는 서버에서 생성
        vo.setUserId(userId);
        vo.setCreatedBy(userId);

        mapper.create(vo);
        log.info("예금 등록 완료 : userDepositId={}", vo.getUserDepositId());

        return vo.getUserDepositId();
    }

    @Transactional
    @Override
    public void update(Long userDepositId, DepositRequestDTO request) {
        request.validate();

        Long userId = LoginUser.getUserId();

        UserDepositVO vo = request.toVO();
        vo.setUserDepositId(userDepositId);
        vo.setUserId(userId);
        vo.setUpdatedBy(userId);

        // 0건이면 대상이 없거나 다른 사용자의 예금
        if (mapper.update(vo) == 0) {
            throw new DepositNotFoundException();
        }
        log.info("예금 수정 완료 : userDepositId={}", userDepositId);
    }

    @Transactional
    @Override
    public void delete(Long userDepositId) {
        if (mapper.softDelete(userDepositId, LoginUser.getUserId()) == 0) {
            throw new DepositNotFoundException();
        }
        log.info("예금 삭제 완료 : userDepositId={}", userDepositId);
    }
}
