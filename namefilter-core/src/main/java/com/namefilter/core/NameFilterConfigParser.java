package com.namefilter.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Converts platform configuration values into the shared immutable model.
 * Keys are case-insensitive and ignore punctuation, so both
 * {@code rejection-message} and {@code rejectionMessage} are accepted.
 */
public final class NameFilterConfigParser {
    private NameFilterConfigParser() {
    }

    public static NameFilterConfig parse(Properties properties) {
        Objects.requireNonNull(properties, "properties");
        Map<String, Object> values = new LinkedHashMap<>();
        for (String name : properties.stringPropertyNames()) {
            values.put(name, properties.getProperty(name));
        }
        return parse(values);
    }

    public static NameFilterConfig parse(Map<?, ?> rawValues) {
        Objects.requireNonNull(rawValues, "rawValues");
        Map<String, Object> values = normalizeKeys(rawValues);
        NameFilterConfig.Builder builder = NameFilterConfig.builder();

        Object mode = first(values, "mode", "handlingmode", "action");
        if (mode != null) {
            builder.handlingMode(HandlingMode.parse(String.valueOf(mode)));
        }

        Object message = first(values, "rejectionmessage", "kickmessage", "message");
        if (message != null && !String.valueOf(message).isBlank()) {
            builder.rejectionMessage(String.valueOf(message));
        }

        Object namePattern = first(values,
                "enforcenamepattern",
                "namepatternenabled",
                "checknamepattern",
                "validatenamecharacters",
                "validatenamepattern");
        if (namePattern != null) {
            builder.enforceNamePattern(parseBoolean(namePattern, true));
        }
        return builder.build();
    }

    private static Map<String, Object> normalizeKeys(Map<?, ?> source) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                normalized.put(normalizeKey(String.valueOf(entry.getKey())), entry.getValue());
            }
        }
        return normalized;
    }

    private static String normalizeKey(String key) {
        StringBuilder normalized = new StringBuilder(key.length());
        for (int i = 0; i < key.length(); i++) {
            char character = key.charAt(i);
            if (Character.isLetterOrDigit(character)) {
                normalized.append(Character.toLowerCase(character));
            }
        }
        return normalized.toString();
    }

    private static Object first(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            if (values.containsKey(key)) {
                return values.get(key);
            }
        }
        return null;
    }

    /**
     * Parse booleans consistently across YAML values and Java properties.
     * Unknown values retain the secure default instead of silently disabling
     * username validation.
     */
    private static boolean parseBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        String text = String.valueOf(value).trim().toLowerCase(java.util.Locale.ROOT);
        return switch (text) {
            case "true", "yes", "on", "1" -> true;
            case "false", "no", "off", "0" -> false;
            default -> defaultValue;
        };
    }
}
