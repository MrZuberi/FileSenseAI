package com.filesenseai.web;

import java.util.List;

public record FolderListing(String currentPath, String parentPath, List<FolderEntry> subFolders, int fileCount) {

    public record FolderEntry(String name, String path) {
    }
}