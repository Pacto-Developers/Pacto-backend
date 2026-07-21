package com.pacto.api.file.service;

import com.pacto.api.file.domain.FileCategory;

import java.util.UUID;

public class ObjectKeyGenerator {

    private ObjectKeyGenerator() {
    }

    public static String generate(FileCategory category, Long ownerId, String originalFilename) {
        String extension = FileExtensions.extract(originalFilename);
        String fileName = extension.isEmpty()
                ? UUID.randomUUID().toString()
                : UUID.randomUUID() + "." + extension;
        return category.prefix(ownerId) + "/" + fileName;
    }
}
