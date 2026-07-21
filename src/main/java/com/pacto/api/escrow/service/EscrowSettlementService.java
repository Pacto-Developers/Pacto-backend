package com.pacto.api.escrow.service;

import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.common.exception.CampaignNotFoundException;
import com.pacto.api.common.exception.EscrowNotFoundException;
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
public class EscrowSettlementService {

    private final EscrowLedgerRepository escrowLedgerRepository;
    private final WalletRepository walletRepository;
    private final CampaignRepository campaignRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public void release(Long escrowId) {
        EscrowLedger escrow = getEscrow(escrowId);
        escrow.release();

        Campaign campaign = getCampaign(escrow.getCampaignId());
        Wallet advertiserWallet = getWallet(campaign.getAdvertiserId());
        Wallet bloggerWallet = getWallet(escrow.getBloggerId());

        advertiserWallet.decreaseLockedBalance(escrow.getAmount());
        bloggerWallet.addBalance(escrow.getAmount());

        walletRepository.save(advertiserWallet);
        walletRepository.save(bloggerWallet);
        escrowLedgerRepository.save(escrow);
        pointHistoryRepository.save(PointHistory.create(
                bloggerWallet,
                escrow.getAmount(),
                PointHistoryType.RELEASE,
                escrow.getEscrowId(),
                PointHistoryReferenceType.ESCROW
        ));
    }

    @Transactional
    public void cancel(Long escrowId) {
        EscrowLedger escrow = getEscrow(escrowId);
        escrow.cancel();

        Campaign campaign = getCampaign(escrow.getCampaignId());
        Wallet advertiserWallet = getWallet(campaign.getAdvertiserId());

        advertiserWallet.refundLockedBalance(escrow.getAmount());

        walletRepository.save(advertiserWallet);
        escrowLedgerRepository.save(escrow);
        pointHistoryRepository.save(PointHistory.create(
                advertiserWallet,
                escrow.getAmount(),
                PointHistoryType.REFUND,
                escrow.getEscrowId(),
                PointHistoryReferenceType.ESCROW
        ));
    }

    private EscrowLedger getEscrow(Long escrowId) {
        return escrowLedgerRepository.findById(escrowId)
                .orElseThrow(EscrowNotFoundException::new);
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
