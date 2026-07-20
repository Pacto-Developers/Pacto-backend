package com.pacto.api.file.service;

class FileExtensions {

    private FileExtensions() {
    }

    static String extract(String filename) {
        int dotIndex = filename == null ? -1 : filename.lastIndexOf('.');
        if (filename == null || dotIndex == -1 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }
}
