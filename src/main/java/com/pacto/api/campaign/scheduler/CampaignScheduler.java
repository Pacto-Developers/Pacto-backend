package com.pacto.api.campaign.scheduler;

import com.pacto.api.application.domain.Application;
import com.pacto.api.application.domain.ApplicationStatus;
import com.pacto.api.application.repository.ApplicationRepository;
import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.domain.CampaignStatus;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.escrow.service.EscrowLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignScheduler {

    private final CampaignRepository campaignRepository;
    private final ApplicationRepository applicationRepository;
    private final EscrowLockService escrowLockService;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void closeExpiredCampaigns() {
        List<Campaign> expired = campaignRepository.findByDeadlineBeforeAndStatus(
                LocalDateTime.now(), CampaignStatus.RECRUITING);

        if (expired.isEmpty()) {
            return;
        }

        log.info("마감 처리할 캠페인 수: {}", expired.size());

        for (Campaign campaign : expired) {
            campaign.close();

            List<Application> pending = applicationRepository.findByCampaignIdAndStatus(
                    campaign.getCampaignId(), ApplicationStatus.PENDING);
            pending.forEach(Application::reject);
            escrowLockService.refundUnusedBudget(campaign.getCampaignId());

            log.info("캠페인 자동 마감 처리 완료 - campaignId: {}, 거절된 신청 수: {}",
                    campaign.getCampaignId(), pending.size());
        }
    }
}
