package com.pacto.api.file.service;

import com.pacto.api.file.domain.FileCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectKeyGeneratorTest {

    @Test
    void 캠페인_썸네일_Object_Key를_생성한다() {
        String objectKey = ObjectKeyGenerator.generate(FileCategory.CAMPAIGN_THUMBNAIL, 7L, "photo.png");

        assertThat(objectKey).matches("campaigns/7/thumbnail/[0-9a-fA-F-]{36}\\.png");
    }

    @Test
    void 캠페인_이미지_Object_Key를_생성한다() {
        String objectKey = ObjectKeyGenerator.generate(FileCategory.CAMPAIGN_IMAGE, 7L, "photo.jpg");

        assertThat(objectKey).matches("campaigns/7/images/[0-9a-fA-F-]{36}\\.jpg");
    }

    @Test
    void 프로필_이미지_Object_Key를_생성한다() {
        String objectKey = ObjectKeyGenerator.generate(FileCategory.PROFILE, 42L, "photo.webp");

        assertThat(objectKey).matches("profiles/42/[0-9a-fA-F-]{36}\\.webp");
    }

    @Test
    void 확장자는_소문자로_저장된다() {
        String objectKey = ObjectKeyGenerator.generate(FileCategory.PROFILE, 42L, "PHOTO.PNG");

        assertThat(objectKey).endsWith(".png");
    }

    @Test
    void 확장자가_없으면_점_없이_UUID만_사용한다() {
        String objectKey = ObjectKeyGenerator.generate(FileCategory.PROFILE, 42L, "noext");

        assertThat(objectKey).matches("profiles/42/[0-9a-fA-F-]{36}");
    }

    @Test
    void 호출할_때마다_서로_다른_Key를_생성한다() {
        String first = ObjectKeyGenerator.generate(FileCategory.PROFILE, 42L, "photo.png");
        String second = ObjectKeyGenerator.generate(FileCategory.PROFILE, 42L, "photo.png");

        assertThat(first).isNotEqualTo(second);
    }
}
