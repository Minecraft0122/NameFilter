package com.namefilter.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Holds one atomically replaceable configuration and matcher for a running plugin. */
public final class NameFilterRuntime {
    private volatile Snapshot snapshot;

    public NameFilterRuntime(NameFilterConfig config, List<String> forbiddenPatterns) {
        replace(config, forbiddenPatterns);
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    /** Load and validate all files before publishing the new runtime state. */
    public void reload(NameFilterConfig config, Path forbiddenPatternsFile) throws IOException {
        Objects.requireNonNull(config, "config");
        List<String> patterns = ForbiddenPatternLoader.load(forbiddenPatternsFile);
        replace(config, patterns);
    }

    private void replace(NameFilterConfig config, List<String> forbiddenPatterns) {
        Objects.requireNonNull(config, "config");
        NameMatcher matcher = new NameMatcher(config, forbiddenPatterns);
        snapshot = new Snapshot(config, matcher);
    }

    public record Snapshot(NameFilterConfig config, NameMatcher matcher) {
        public Snapshot {
            Objects.requireNonNull(config, "config");
            Objects.requireNonNull(matcher, "matcher");
        }
    }
}
