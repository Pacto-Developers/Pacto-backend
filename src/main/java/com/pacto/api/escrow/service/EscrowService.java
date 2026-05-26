package com.pacto.api.escrow.service;

import com.pacto.api.escrow.dto.EscrowLedgerResponse;
import com.pacto.api.escrow.repository.EscrowLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EscrowService {

    private final EscrowLedgerRepository escrowLedgerRepository;

    @Transactional(readOnly = true)
    public List<EscrowLedgerResponse> getMyEscrows(Long bloggerId) {
        return escrowLedgerRepository.findByBloggerId(bloggerId)
                .stream()
                .map(EscrowLedgerResponse::from)
                .toList();
    }
}
