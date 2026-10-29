package com.hrflow.reporting.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Resolves only an authorized job's generated file; never accepts a caller-supplied storage key. */
@Service
@RequiredArgsConstructor
public class PrivateExportDownload {
    private final CsvExportService exports;

    public Download open(UUID tenant, UUID requester, UUID id) {
        var job = exports.getExportJob(tenant, id, requester).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Export not found"));
        if (!"COMPLETED".equals(job.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Export is not completed");
        }
        String name = id + ".csv";
        if (!name.equals(job.getFileName())) throw unavailable();
        try {
            Path root = exports.getStorageRoot().toRealPath();
            Path file = root.resolve(name);
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw unavailable();
            var channel = Files.newByteChannel(file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
            try {
                return new Download(Channels.newInputStream(channel), channel.size(), name);
            } catch (Exception failure) {
                channel.close();
                throw failure;
            }
        } catch (IOException failure) {
            throw unavailable();
        }
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Private export file unavailable");
    }

    public record Download(InputStream stream, long length, String name) {}
}
