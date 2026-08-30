package com.namefilter.core;

import java.util.List;

/**
 * Compatibility access to the bundled forbidden-name policy.
 *
 * <p>The authoritative defaults live in {@code forbidden-patterns.txt}. The
 * resource is loaded once when this class is initialized; platform adapters
 * copy it to their data directory and load the user-owned file during
 * startup.</p>
 */
public final class BuiltInForbiddenPatterns {
    private BuiltInForbiddenPatterns() {
    }

    /** Immutable regex fragments loaded from the bundled resource once. */
    public static final List<String> PATTERNS = ForbiddenPatternLoader.loadBundled();
}
