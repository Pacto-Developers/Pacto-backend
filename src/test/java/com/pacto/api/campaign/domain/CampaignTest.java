package com.pacto.api.campaign.domain;

import com.pacto.api.file.exception.FileValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampaignTest {

    private Campaign newCampaign() {
        return new Campaign(1L, "캠페인", null, 50000, Map.of(), LocalDateTime.now().plusDays(7), 3);
    }

    @Test
    void 기존_이미지가_없을_때_5장까지는_허용한다() {
        Campaign campaign = newCampaign();

        assertThatCode(() -> campaign.validateGuidelineImageCapacity(5)).doesNotThrowAnyException();
    }

    @Test
    void 기존_이미지가_없을_때_6장을_요청하면_예외를_던진다() {
        Campaign campaign = newCampaign();

        assertThatThrownBy(() -> campaign.validateGuidelineImageCapacity(6))
                .isInstanceOf(FileValidationException.class)
                .hasMessage("가이드라인 이미지는 최대 5장까지 업로드할 수 있습니다.");
    }

    @Test
    void 기존_이미지와_합쳐서_5장을_넘지_않으면_허용한다() {
        Campaign campaign = newCampaign();
        campaign.addGuidelineImages(List.of("k1", "k2", "k3"));

        assertThatCode(() -> campaign.validateGuidelineImageCapacity(2)).doesNotThrowAnyException();
    }

    @Test
    void 기존_이미지와_합쳐서_5장을_넘으면_예외를_던진다() {
        Campaign campaign = newCampaign();
        campaign.addGuidelineImages(List.of("k1", "k2", "k3"));

        assertThatThrownBy(() -> campaign.validateGuidelineImageCapacity(3))
                .isInstanceOf(FileValidationException.class)
                .hasMessage("가이드라인 이미지는 최대 5장까지 업로드할 수 있습니다.");
    }

    @Test
    void addGuidelineImages는_한도를_넘으면_추가하지_않고_예외를_던진다() {
        Campaign campaign = newCampaign();
        campaign.addGuidelineImages(List.of("k1", "k2", "k3", "k4", "k5"));

        assertThatThrownBy(() -> campaign.addGuidelineImages(List.of("k6")))
                .isInstanceOf(FileValidationException.class);
        assertThatCode(() -> campaign.validateGuidelineImageCapacity(0)).doesNotThrowAnyException();
    }
}
