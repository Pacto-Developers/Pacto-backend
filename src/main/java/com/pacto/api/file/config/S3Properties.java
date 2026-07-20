package com.pacto.api.file.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cloud.aws")
public class S3Properties {

    private String region;
    private S3 s3 = new S3();

    @Getter
    @Setter
    public static class S3 {
        private String bucket;
    }
}
