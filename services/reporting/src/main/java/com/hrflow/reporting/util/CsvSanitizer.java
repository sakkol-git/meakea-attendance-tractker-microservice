package com.hrflow.reporting.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class CsvSanitizer {

    private static final String FORMULA_TRIGGERS = "=+-@\t\r";

    private CsvSanitizer() {}

    /**
     * Prevents CSV injection (formula injection) by escaping formula-leading cells.
     * If a cell starts with =, +, -, @, \t, or \r (including after leading whitespace), prefix with a single quote.
     */
    public static String escapeCell(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.startsWith("'")) {
            return value;
        }
        char rawFirst = value.charAt(0);
        if (FORMULA_TRIGGERS.indexOf(rawFirst) >= 0) {
            return "'" + value;
        }
        String trimmed = value.stripLeading();
        if (!trimmed.isEmpty()) {
            char trimmedFirst = trimmed.charAt(0);
            if (FORMULA_TRIGGERS.indexOf(trimmedFirst) >= 0) {
                return "'" + value;
            }
        }
        return value;
    }

    /**
     * Formats and escapes a single cell according to RFC 4180 and formula injection prevention.
     * Encloses the cell in double quotes if it contains commas, quotes, newlines, or tabs,
     * doubling any internal double quotes.
     */
    public static String formatCell(String value) {
        String sanitized = escapeCell(value);
        if (sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n") || sanitized.contains("\r") || sanitized.contains("\t")) {
            return "\"" + sanitized.replace("\"", "\"\"") + "\"";
        }
        return sanitized;
    }

    /**
     * Formats a row of cells into a sanitized RFC 4180 CSV row.
     */
    public static String formatRow(List<String> cells) {
        if (cells == null || cells.isEmpty()) {
            return "";
        }
        return cells.stream()
                .map(CsvSanitizer::formatCell)
                .collect(Collectors.joining(","));
    }

    /**
     * Parses a raw CSV line into constituent cells respecting RFC 4180 quoting and escaping rules.
     */
    public static List<String> parseLine(String line) {
        if (line == null) {
            return List.of();
        }
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int len = line.length();

        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++; // skip escaped double quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    cells.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        cells.add(current.toString());
        return cells;
    }

    /**
     * Sanitizes a raw CSV line by parsing constituent cells, escaping formula triggers, and re-encoding with proper quotes.
     */
    public static String sanitizeLine(String line) {
        if (line == null) {
            return "";
        }
        List<String> cells = parseLine(line);
        return formatRow(cells);
    }
}
