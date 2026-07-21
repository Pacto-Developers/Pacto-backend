package com.pacto.api.file.domain;

public enum FileCategory {
    CAMPAIGN_THUMBNAIL("campaigns/%d/thumbnail"),
    CAMPAIGN_IMAGE("campaigns/%d/images"),
    PROFILE("profiles/%d");

    private final String keyPrefixFormat;

    FileCategory(String keyPrefixFormat) {
        this.keyPrefixFormat = keyPrefixFormat;
    }

    public String prefix(Long ownerId) {
        return String.format(keyPrefixFormat, ownerId);
    }
}
