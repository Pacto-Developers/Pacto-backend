package com.pacto.api.escrow.service;

import com.pacto.api.auth.entity.User;
import com.pacto.api.auth.repository.UserRepository;
import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.common.dto.PageResponse;
import com.pacto.api.common.exception.CampaignNotFoundException;
import com.pacto.api.common.exception.UserNotFoundException;
import com.pacto.api.escrow.dto.EscrowLedgerResponse;
import com.pacto.api.escrow.entity.EscrowLedger;
import com.pacto.api.escrow.entity.EscrowStatus;
import com.pacto.api.escrow.repository.EscrowLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EscrowService {

    private final EscrowLedgerRepository escrowLedgerRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;

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

    @Transactional(readOnly = true)
    public List<EscrowLedgerResponse> getAdvertiserCampaignEscrows(Long advertiserId, Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(CampaignNotFoundException::new);
        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new AccessDeniedException("접근 권한이 없습니다.");
        }

        return escrowLedgerRepository.findByCampaignId(campaignId)
                .stream()
                .map(escrow -> EscrowLedgerResponse.from(escrow, campaign, getBlogger(escrow.getBloggerId())))
                .toList();
    }

    private User getBlogger(Long bloggerId) {
        return userRepository.findById(bloggerId)
                .orElseThrow(UserNotFoundException::new);
    }
}
