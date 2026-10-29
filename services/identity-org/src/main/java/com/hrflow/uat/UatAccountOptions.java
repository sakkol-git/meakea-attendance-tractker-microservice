package com.hrflow.uat;

import com.hrflow.identityorg.auth.setup.UatAccountRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

final class UatAccountOptions {
    private static final Set<String> NAMES = Set.of("tenant", "employee", "email", "roles", "time-zone");
    private UatAccountOptions() {}

    static UatAccountRequest parse(String[] args) {
        var values = new HashMap<String, String>();
        for (String arg : args) {
            int equals = arg.indexOf('=');
            if (!arg.startsWith("--") || equals < 3 || !NAMES.contains(arg.substring(2, equals)))
                throw new IllegalArgumentException("Unsupported setup option. Use --help; never pass passwords as arguments");
            String key = arg.substring(2, equals), value = arg.substring(equals + 1);
            if (value.isBlank() || values.putIfAbsent(key, value) != null)
                throw new IllegalArgumentException("Every setup option must occur once with a nonblank value");
        }
        if (!values.keySet().equals(NAMES)) throw new IllegalArgumentException("All five explicit setup options are required; use --help");
        return new UatAccountRequest(values.get("tenant"), UUID.fromString(values.get("employee")), values.get("email"),
                Arrays.stream(values.get("roles").split(",", -1)).map(String::strip).toList(), values.get("time-zone"));
    }
}
