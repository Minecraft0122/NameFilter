package com.namefilter.core;

import java.util.Objects;

/** Immutable, platform-independent runtime configuration. */
public final class NameFilterConfig {
    public static final String DEFAULT_REJECTION_MESSAGE =
            "Your name contains a forbidden word.";
    /** The default character policy applied to player names. */
    public static final String DEFAULT_NAME_PATTERN = "[a-zA-Z0-9_]*";

    private final HandlingMode handlingMode;
    private final String rejectionMessage;
    private final boolean enforceNamePattern;

    private NameFilterConfig(Builder builder) {
        this.handlingMode = Objects.requireNonNull(builder.handlingMode, "handlingMode");
        this.rejectionMessage = requireNonBlank(builder.rejectionMessage, "rejectionMessage");
        this.enforceNamePattern = builder.enforceNamePattern;
    }

    public static NameFilterConfig defaults() {
        return builder().build();
    }

    public HandlingMode handlingMode() {
        return handlingMode;
    }

    public HandlingMode getHandlingMode() {
        return handlingMode();
    }

    public HandlingMode mode() {
        return handlingMode();
    }

    public HandlingMode getMode() {
        return handlingMode();
    }

    public String rejectionMessage() {
        return rejectionMessage;
    }

    public String getRejectionMessage() {
        return rejectionMessage();
    }

    public String message() {
        return rejectionMessage();
    }

    public String getMessage() {
        return rejectionMessage();
    }

    /**
     * Whether names must consist only of ASCII letters, digits, and underscores.
     * The policy is enabled by default and corresponds to
     * {@link #DEFAULT_NAME_PATTERN}.
     */
    public boolean enforceNamePattern() {
        return enforceNamePattern;
    }

    public boolean isNamePatternEnforced() {
        return enforceNamePattern();
    }

    public boolean getEnforceNamePattern() {
        return enforceNamePattern();
    }

    public boolean isEnforceNamePattern() {
        return enforceNamePattern();
    }

    /** Alias for integrations that describe the option as character validation. */
    public boolean validateNameCharacters() {
        return enforceNamePattern();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private HandlingMode handlingMode = HandlingMode.MESSAGE;
        private String rejectionMessage = DEFAULT_REJECTION_MESSAGE;
        private boolean enforceNamePattern = true;

        public Builder handlingMode(HandlingMode handlingMode) {
            this.handlingMode = Objects.requireNonNull(handlingMode, "handlingMode");
            return this;
        }

        public Builder mode(HandlingMode handlingMode) {
            return handlingMode(handlingMode);
        }

        public Builder rejectionMessage(String rejectionMessage) {
            this.rejectionMessage = rejectionMessage;
            return this;
        }

        public Builder message(String message) {
            return rejectionMessage(message);
        }

        /** Enable or disable the default ASCII username character policy. */
        public Builder enforceNamePattern(boolean enforceNamePattern) {
            this.enforceNamePattern = enforceNamePattern;
            return this;
        }

        public Builder namePatternEnforced(boolean enforceNamePattern) {
            return enforceNamePattern(enforceNamePattern);
        }

        public Builder validateNameCharacters(boolean validateNameCharacters) {
            return enforceNamePattern(validateNameCharacters);
        }

        public NameFilterConfig build() {
            return new NameFilterConfig(this);
        }
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
