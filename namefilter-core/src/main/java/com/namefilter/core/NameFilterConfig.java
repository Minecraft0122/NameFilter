package com.namefilter.core;

import java.util.Objects;

/** Immutable, platform-independent runtime configuration. */
public final class NameFilterConfig {
    public static final String DEFAULT_REJECTION_MESSAGE =
            "Your name contains a forbidden word.";

    private final HandlingMode handlingMode;
    private final String rejectionMessage;

    private NameFilterConfig(Builder builder) {
        this.handlingMode = Objects.requireNonNull(builder.handlingMode, "handlingMode");
        this.rejectionMessage = requireNonBlank(builder.rejectionMessage, "rejectionMessage");
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private HandlingMode handlingMode = HandlingMode.MESSAGE;
        private String rejectionMessage = DEFAULT_REJECTION_MESSAGE;

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
