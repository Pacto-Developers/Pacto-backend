package com.pacto.api.escrow.service;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.common.exception.CampaignNotFoundException;
import com.pacto.api.common.exception.WalletNotFoundException;
import com.pacto.api.escrow.entity.EscrowLedger;
import com.pacto.api.escrow.repository.EscrowLedgerRepository;
import com.pacto.api.wallet.entity.PointHistory;
import com.pacto.api.wallet.entity.PointHistoryReferenceType;
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
    public void lockCampaignBudget(Campaign campaign) {
        Wallet advertiserWallet = getWallet(campaign.getAdvertiserId());
        int totalBudget = campaign.getRewardPoint() * campaign.getTotalSlots();

        advertiserWallet.lockBalance(totalBudget);
        walletRepository.save(advertiserWallet);
        pointHistoryRepository.save(PointHistory.create(
                advertiserWallet,
                -totalBudget,
                PointHistoryType.LOCK,
                campaign.getCampaignId(),
                PointHistoryReferenceType.CAMPAIGN
        ));
    }

    @Transactional
    public Long createEscrowForSelection(Long campaignId, Long bloggerId) {
        Campaign campaign = getCampaign(campaignId);
        int amount = campaign.getRewardPoint();

        EscrowLedger escrow = EscrowLedger.create(campaignId, bloggerId, amount);
        EscrowLedger savedEscrow = escrowLedgerRepository.save(escrow);
        return savedEscrow.getEscrowId();
    }

    @Transactional
    public Long lock(Long campaignId, Long bloggerId) {
        return createEscrowForSelection(campaignId, bloggerId);
    }

    @Transactional
    public void refundUnusedBudget(Long campaignId) {
        Campaign campaign = getCampaign(campaignId);
        int unusedBudget = campaign.calculateRemainingBudget();
        if (unusedBudget <= 0) {
            return;
        }

        Wallet advertiserWallet = getWallet(campaign.getAdvertiserId());

        advertiserWallet.refundLockedBalance(unusedBudget);
        campaign.clearRemainingSlots();

        walletRepository.save(advertiserWallet);
        campaignRepository.save(campaign);
        pointHistoryRepository.save(PointHistory.create(
                advertiserWallet,
                unusedBudget,
                PointHistoryType.REFUND,
                campaign.getCampaignId(),
                PointHistoryReferenceType.CAMPAIGN
        ));
    }

    private Campaign getCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(CampaignNotFoundException::new);
    }

    private Wallet getWallet(Long userId) {
        return walletRepository.findWithLockByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);
    }
}
