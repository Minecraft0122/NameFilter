package com.namefilter.core;

/**
 * The user-visible behavior when a name matches a forbidden entry.
 */
public enum HandlingMode {
    /** Reject the connection and send the configured rejection message. */
    MESSAGE,
    /** Reject the connection without sending a reason to the player. */
    SILENT;

    /** Parse a mode from configuration, defaulting to {@link #MESSAGE}. */
    public static HandlingMode parse(String value) {
        if (value == null) {
            return MESSAGE;
        }
        return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "silent", "quiet", "hide" -> SILENT;
            default -> MESSAGE;
        };
    }
}
