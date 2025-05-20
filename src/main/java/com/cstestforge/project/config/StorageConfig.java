package com.cstestforge.project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;

/**
 * Configuration for the file storage system.
 */
@Configuration
public class StorageConfig {
    
    @Value("${app.storage.root:./storage}")
    private String storageRoot;

    @PostConstruct
    public void init() {
        // Create root storage directory if it doesn't exist
        File storageDir = new File(storageRoot);
        if (!storageDir.exists()) {
            boolean created = storageDir.mkdirs();
            if (!created) {
                System.err.println("Failed to create storage directory: " + storageRoot);
            }
        }

        // Create all required subdirectories
        String[] requiredDirs = {
                "projects",
                "tags",
                "_metadata",
                "projects/_index",
                "environments",
                "recordings",
                "execution",
                "code-builder",
                "api-testing",
                "export",
                "reports",
                "ado",
                "dashboard"
        };

        for (String dir : requiredDirs) {
            createDirectory(dir);
        }

        // Create initial index files
        createIndexFile("projects/_index.json", "{}");
        createIndexFile("tags/_index.json", "[]");
    }

    private void createDirectory(String path) {
        File dir = new File(storageRoot, path);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                System.err.println("Failed to create directory: " + path);
            }
        }
    }

    private void createIndexFile(String path, String defaultContent) {
        File file = new File(storageRoot, path);
        if (!file.exists()) {
            try {
                // Ensure parent directory exists
                File parentDir = file.getParentFile();
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }

                // Write default content
                java.nio.file.Files.write(
                        file.toPath(),
                        defaultContent.getBytes(),
                        java.nio.file.StandardOpenOption.CREATE
                );
            } catch (Exception e) {
                System.err.println("Failed to create index file: " + path + ", error: " + e.getMessage());
            }
        }
    }
} 