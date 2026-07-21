package com.pacto.api.wallet.service;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.escrow.entity.EscrowLedger;
import com.pacto.api.escrow.repository.EscrowLedgerRepository;
import com.pacto.api.wallet.dto.PointHistoryResponse;
import com.pacto.api.wallet.entity.PointHistory;
import com.pacto.api.wallet.entity.PointHistoryReferenceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PointHistoryResponseMapper {

    private final CampaignRepository campaignRepository;
    private final EscrowLedgerRepository escrowLedgerRepository;

    public PointHistoryResponse toResponse(PointHistory history) {
        return PointHistoryResponse.from(history, resolveCampaign(history).orElse(null));
    }

    private Optional<Campaign> resolveCampaign(PointHistory history) {
        if (history.getReferenceId() == null) {
            return Optional.empty();
        }

        PointHistoryReferenceType referenceType = history.getReferenceType() == null
                ? PointHistoryReferenceType.infer(history.getType())
                : history.getReferenceType();

        return switch (referenceType) {
            case CAMPAIGN -> campaignRepository.findById(history.getReferenceId());
            case ESCROW -> escrowLedgerRepository.findById(history.getReferenceId())
                    .flatMap(this::findCampaignByEscrow);
            case PAYMENT, PAYMENT_REFUND, WITHDRAWAL -> Optional.empty();
        };
    }

    private Optional<Campaign> findCampaignByEscrow(EscrowLedger escrow) {
        return campaignRepository.findById(escrow.getCampaignId());
    }
}
