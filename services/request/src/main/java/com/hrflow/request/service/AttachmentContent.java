package com.hrflow.request.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Small allowlist/signature check, not antivirus scanning or proof of document validity. */
final class AttachmentContent {
    private AttachmentContent() {}

    static void name(String name, String mime) {
        if (name == null || name.length() > 255 || name.codePoints().anyMatch(Character::isISOControl))
            throw invalid("Invalid attachment filename");
        String lower = name.toLowerCase(Locale.ROOT), media = mime.toLowerCase(Locale.ROOT);
        boolean extension = switch (media) {
            case "application/pdf" -> lower.endsWith(".pdf");
            case "image/png" -> lower.endsWith(".png");
            case "image/jpeg", "image/jpg" -> lower.endsWith(".jpg") || lower.endsWith(".jpeg");
            default -> false;
        };
        if (!extension) throw invalid("File extension does not match the allowed content type");
    }

    static void bytes(Path file, String mime) throws IOException {
        byte[] signature;
        try (var input = Files.newInputStream(file)) { signature = input.readNBytes(8); }
        byte[] expected = switch (mime.toLowerCase(Locale.ROOT)) {
            case "application/pdf" -> new byte[]{'%', 'P', 'D', 'F', '-'};
            case "image/png" -> new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
            case "image/jpeg", "image/jpg" -> new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff};
            default -> throw invalid("Unsupported content type");
        };
        if (signature.length < expected.length || !java.util.Arrays.equals(expected, java.util.Arrays.copyOf(signature, expected.length)))
            throw invalid("File content signature does not match the reservation");
    }

    private static ResponseStatusException invalid(String detail) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, detail);
    }
}
