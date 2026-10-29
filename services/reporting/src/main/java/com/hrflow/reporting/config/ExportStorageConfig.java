package com.hrflow.reporting.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExportStorageConfig {

    @Bean
    public Path exportStorageRoot(
            @Value("${hrflow.reporting.export.storage-dir:${java.io.tmpdir}/hrflow-exports}") String storageDir
    ) {
        Path root = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            if (Files.getFileStore(root).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(root, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create export storage root: " + root, e);
        }
        return root;
    }
}
