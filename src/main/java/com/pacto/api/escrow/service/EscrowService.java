package com.pacto.api.escrow.service;

import com.pacto.api.common.dto.PageResponse;
import com.pacto.api.escrow.dto.EscrowLedgerResponse;
import com.pacto.api.escrow.entity.EscrowLedger;
import com.pacto.api.escrow.entity.EscrowStatus;
import com.pacto.api.escrow.repository.EscrowLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EscrowService {

    private final EscrowLedgerRepository escrowLedgerRepository;

    @Transactional(readOnly = true)
    public PageResponse<EscrowLedgerResponse> getMyEscrows(Long bloggerId, EscrowStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 1) - 1,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<EscrowLedger> escrows = status == null
                ? escrowLedgerRepository.findByBloggerId(bloggerId, pageRequest)
                : escrowLedgerRepository.findByBloggerIdAndStatus(bloggerId, status, pageRequest);
        return PageResponse.from(escrows, EscrowLedgerResponse::from);
    }
}
