package com.hrflow.request.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttachmentStorageService {

    private final Path storageRoot;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/jpg"
    );

    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MiB

    public AttachmentStorageService(@Value("${attachment.storage.dir:/tmp/hrflow-attachments}") String storageDir) {
        this.storageRoot = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize storage directory", e);
        }
    }

    public Path getStorageRoot() {
        return storageRoot;
    }

    public void validateAttachmentMetadata(String fileName, String contentType, long fileSize) {
        if (fileName == null || fileName.isBlank() || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name: path traversal rejected");
        }
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Unsupported content type: " + contentType);
        }
        AttachmentContent.name(fileName, contentType);
        if (fileSize <= 0 || fileSize > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "File size must be between 1 and 10485760 bytes");
        }
    }

    public String generateStorageKey(String tenantId, String requestId, String attachmentId) {
        return tenantId + "/" + requestId + "/" + attachmentId + ".bin";
    }

    public Path resolvePath(String storageKey) {
        if (storageKey == null || storageKey.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path traversal forbidden");
        }
        Path path = storageRoot.resolve(storageKey).normalize();
        if (!path.startsWith(storageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path traversal forbidden");
        }
        return path;
    }

    public void storeFile(String storageKey, InputStream inputStream, String expectedChecksum, long expectedSize, String expectedContentType) throws IOException {
        Path targetPath = resolvePath(storageKey);
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path tempPath = Files.createTempFile(parent, targetPath.getFileName().toString() + ".", ".tmp");

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }

        long bytesWritten = 0;
        try (DigestInputStream dis = new DigestInputStream(inputStream, digest);
             OutputStream os = Files.newOutputStream(tempPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = dis.read(buffer)) != -1) {
                bytesWritten += read;
                if (bytesWritten > MAX_FILE_SIZE || bytesWritten > expectedSize) {
                    Files.deleteIfExists(tempPath);
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "File exceeds maximum size of 10MB");
                }
                os.write(buffer, 0, read);
            }
        } catch (Exception ex) {
            Files.deleteIfExists(tempPath);
            throw ex;
        }

        String calculatedChecksum = HexFormat.of().formatHex(digest.digest());
        if (expectedChecksum != null && !expectedChecksum.isBlank()
                && !expectedChecksum.equalsIgnoreCase(calculatedChecksum)) {
            Files.deleteIfExists(tempPath);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Checksum mismatch: file corrupted or tampered");
        }

        if (expectedSize > 0 && bytesWritten != expectedSize) {
            Files.deleteIfExists(tempPath);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Size mismatch");
        }

        try {
            AttachmentContent.bytes(tempPath, expectedContentType);
            Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(tempPath); }
    }

    public byte[] readFile(String storageKey) throws IOException {
        Path path = resolvePath(storageKey);
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment file not found on disk");
        }
        return Files.readAllBytes(path);
    }

    public boolean fileExists(String storageKey) {
        Path path = resolvePath(storageKey);
        return Files.exists(path);
    }

    public int cleanOrphanTempFiles(Duration maxAge) throws IOException {
        Instant cutoff = Instant.now().minus(maxAge);
        int deleted = 0;
        try (var stream = Files.walk(storageRoot)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                if (path.getFileName().toString().endsWith(".tmp")) {
                    Instant lastModified = Files.getLastModifiedTime(path).toInstant();
                    if (lastModified.isBefore(cutoff)) {
                        Files.deleteIfExists(path);
                        deleted++;
                    }
                }
            }
        }
        return deleted;
    }
}
