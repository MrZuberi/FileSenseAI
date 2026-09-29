package com.filesenseai.web;

import java.util.List;

public record FolderListing(String currentPath, List<FolderEntry> subFolders, List<FileEntry> files, boolean isDriveList) {

    public record FolderEntry(String name, String path) {
    }

    public record FileEntry(String name, String extension, String sizeLabel) {
    }
}