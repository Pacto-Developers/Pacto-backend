package com.pacto.api.escrow.service;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.common.exception.CampaignNotFoundException;
import com.pacto.api.common.exception.WalletNotFoundException;
import com.pacto.api.escrow.entity.EscrowLedger;
import com.pacto.api.escrow.repository.EscrowLedgerRepository;
import com.pacto.api.wallet.entity.PointHistory;
import com.pacto.api.wallet.entity.PointHistoryType;
import com.pacto.api.wallet.entity.Wallet;
import com.pacto.api.wallet.repository.PointHistoryRepository;
import com.pacto.api.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EscrowLockService {

    private final EscrowLedgerRepository escrowLedgerRepository;
    private final WalletRepository walletRepository;
    private final CampaignRepository campaignRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public Long lock(Long campaignId, Long bloggerId) {
        Campaign campaign = getCampaign(campaignId);
        Wallet advertiserWallet = getWallet(campaign.getAdvertiserId());
        int amount = campaign.getRewardPoint();

        advertiserWallet.lockBalance(amount);
        walletRepository.save(advertiserWallet);

        EscrowLedger escrow = EscrowLedger.create(campaignId, bloggerId, amount);
        EscrowLedger savedEscrow = escrowLedgerRepository.save(escrow);
        pointHistoryRepository.save(PointHistory.create(
                advertiserWallet,
                -amount,
                PointHistoryType.LOCK,
                savedEscrow.getEscrowId()
        ));

        return savedEscrow.getEscrowId();
    }

    private Campaign getCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(CampaignNotFoundException::new);
    }

    private Wallet getWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);
    }
}
