package com.namefilter.core;

import java.time.Instant;
import java.util.Objects;

/** One rejected connection written to the daily audit log. */
public record AuditRecord(
        Instant timestamp,
        String platform,
        String playerName,
        String remoteAddress,
        String matchedText,
        HandlingMode handlingMode
) {
    public AuditRecord {
        Objects.requireNonNull(timestamp, "timestamp");
        platform = requireText(platform, "platform");
        playerName = requireText(playerName, "playerName");
        remoteAddress = remoteAddress == null ? "-" : remoteAddress;
        matchedText = requireText(matchedText, "matchedText");
        Objects.requireNonNull(handlingMode, "handlingMode");
    }

    public static AuditRecord blocked(
            String platform,
            String playerName,
            String remoteAddress,
            MatchResult match,
            HandlingMode handlingMode
    ) {
        Objects.requireNonNull(match, "match");
        if (!match.isMatch()) {
            throw new IllegalArgumentException("Cannot audit a non-matching result");
        }
        return new AuditRecord(
                Instant.now(), platform, playerName, remoteAddress,
                match.matchedText(), handlingMode);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        return value;
    }
}
