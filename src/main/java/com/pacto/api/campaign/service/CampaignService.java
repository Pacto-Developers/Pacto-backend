package com.pacto.api.campaign.service;

import com.pacto.api.application.domain.Application;
import com.pacto.api.application.domain.ApplicationStatus;
import com.pacto.api.application.repository.ApplicationRepository;
import com.pacto.api.campaign.domain.Campaign;
import com.pacto.api.campaign.domain.CampaignStatus;
import com.pacto.api.campaign.dto.CampaignRequestDto;
import com.pacto.api.campaign.dto.CampaignResponseDto;
import com.pacto.api.campaign.repository.CampaignRepository;
import com.pacto.api.common.exception.CampaignAccessDeniedException;
import com.pacto.api.common.exception.CampaignNotFoundException;
import com.pacto.api.common.exception.InvalidCampaignStatusException;
import com.pacto.api.escrow.service.EscrowLockService;
import com.pacto.api.file.domain.FileCategory;
import com.pacto.api.file.exception.FileValidationException;
import com.pacto.api.file.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private static final List<CampaignStatus> HIDDEN_FROM_LIST = List.of(
            CampaignStatus.CLOSED, CampaignStatus.CANCELLED
    );
    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofMinutes(10);

    private final CampaignRepository campaignRepository;
    private final ApplicationRepository applicationRepository;
    private final EscrowLockService escrowLockService;
    private final FileUploadService fileUploadService;

    // 캠페인 목록 조회 (필터 없으면 취소/마감 캠페인 제외)
    @Transactional(readOnly = true)
    public Page<Campaign> getCampaigns(CampaignStatus status, Pageable pageable) {
        if (status != null) {
            return campaignRepository.findByStatus(status, pageable);
        }
        return campaignRepository.findByStatusNotIn(HIDDEN_FROM_LIST, pageable);
    }

    // 캠페인 상세 조회
    @Transactional(readOnly = true)
    public Campaign getCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException());
    }

    // 캠페인 응답 변환 (Object Key → Presigned URL)
    public CampaignResponseDto toResponseDto(Campaign campaign) {
        String thumbnailUrl = resolveThumbnailUrl(campaign.getThumbnailUrl());
        List<String> guidelineImageUrls = campaign.getGuidelineImageKeys().stream()
                .map(key -> fileUploadService.getPresignedUrl(key, PRESIGNED_URL_EXPIRATION).toString())
                .toList();
        return CampaignResponseDto.from(campaign, thumbnailUrl, guidelineImageUrls);
    }

    private String resolveThumbnailUrl(String thumbnailKey) {
        if (thumbnailKey == null || thumbnailKey.isBlank()) {
            return null;
        }
        return fileUploadService.getPresignedUrl(thumbnailKey, PRESIGNED_URL_EXPIRATION).toString();
    }

    // 캠페인 등록
    @Transactional
    public Campaign createCampaign(CampaignRequestDto dto, Long advertiserId) {
        Campaign campaign = new Campaign(
                advertiserId,
                dto.getTitle(),
                null,
                dto.getRewardPoint(),
                dto.getGuidelines(),
                dto.getDeadline(),
                dto.getTotalSlots()
        );
        Campaign savedCampaign = campaignRepository.save(campaign);
        escrowLockService.lockCampaignBudget(savedCampaign);
        return savedCampaign;
    }

    // 캠페인 썸네일 업로드
    @Transactional
    public Campaign uploadThumbnail(Long campaignId, Long advertiserId, MultipartFile file) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(CampaignNotFoundException::new);
        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new CampaignAccessDeniedException();
        }

        String previousThumbnailKey = campaign.getThumbnailUrl();
        String objectKey = fileUploadService.upload(FileCategory.CAMPAIGN_THUMBNAIL, campaignId, file);
        campaign.updateThumbnail(objectKey);

        if (previousThumbnailKey != null && !previousThumbnailKey.isBlank()) {
            fileUploadService.delete(previousThumbnailKey);
        }
        return campaign;
    }

    // 캠페인 가이드라인 이미지 업로드
    @Transactional
    public Campaign uploadGuidelineImages(Long campaignId, Long advertiserId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new FileValidationException("업로드할 파일이 없습니다.");
        }

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(CampaignNotFoundException::new);
        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new CampaignAccessDeniedException();
        }
        campaign.validateGuidelineImageCapacity(files.size());

        List<String> objectKeys = files.stream()
                .map(file -> fileUploadService.upload(FileCategory.CAMPAIGN_IMAGE, campaignId, file))
                .toList();
        campaign.addGuidelineImages(objectKeys);
        return campaign;
    }

    @Transactional
    public Campaign closeCampaign(Long campaignId, Long advertiserId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(CampaignNotFoundException::new);
        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new CampaignAccessDeniedException();
        }
        campaign.closeManually();
        return campaign;
    }

    @Transactional
    public Campaign proceedCampaign(Long campaignId, Long advertiserId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(CampaignNotFoundException::new);
        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new CampaignAccessDeniedException();
        }
        campaign.proceed();
        escrowLockService.refundUnusedBudget(campaignId);
        rejectPendingApplications(campaignId);
        return campaign;
    }

    @Transactional
    public Campaign completeCampaign(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(CampaignNotFoundException::new);
        campaign.complete();
        return campaign;
    }

    @Transactional
    public Campaign cancelCampaign(Long campaignId, Long advertiserId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(CampaignNotFoundException::new);
        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new CampaignAccessDeniedException();
        }
        boolean hasAccepted = !applicationRepository.findByCampaignIdAndStatus(campaignId, ApplicationStatus.ACCEPTED).isEmpty();
        if (hasAccepted) {
            throw new InvalidCampaignStatusException("선정된 블로거가 있어 취소할 수 없습니다.");
        }
        campaign.cancel();
        escrowLockService.refundUnusedBudget(campaignId);
        rejectPendingApplications(campaignId);
        return campaign;
    }

    private void rejectPendingApplications(Long campaignId) {
        List<Application> pending = applicationRepository.findByCampaignIdAndStatus(campaignId, ApplicationStatus.PENDING);
        pending.forEach(Application::reject);
    }
}
