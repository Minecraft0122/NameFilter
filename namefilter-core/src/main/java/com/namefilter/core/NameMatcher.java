package com.namefilter.core;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fast, reusable matcher for player names.
 *
 * <p>All built-in regex fragments are joined into one regular expression. The
 * expression is compiled once, while each check allocates only the JDK's
 * lightweight {@link Matcher} object.</p>
 */
public final class NameMatcher {
    private static final Pattern NEVER_MATCH = Pattern.compile("(?!x)x");
    private static final Pattern DEFAULT_PATTERN = compile(BuiltInForbiddenPatterns.PATTERNS);

    private final Pattern pattern;
    private final List<String> forbiddenPatterns;

    public NameMatcher(NameFilterConfig config) {
        Objects.requireNonNull(config, "config");
        this.forbiddenPatterns = BuiltInForbiddenPatterns.PATTERNS;
        this.pattern = DEFAULT_PATTERN;
    }

    /** Construct a matcher from raw regular-expression fragments. */
    public NameMatcher(Collection<String> forbiddenPatterns) {
        Objects.requireNonNull(forbiddenPatterns, "forbiddenPatterns");
        this.forbiddenPatterns = forbiddenPatterns.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
        this.pattern = compile(this.forbiddenPatterns);
    }

    public static NameMatcher defaults() {
        return new NameMatcher(NameFilterConfig.defaults());
    }

    /** The single compiled built-in regex, useful for diagnostics/metrics. */
    public static Pattern defaultPattern() {
        return DEFAULT_PATTERN;
    }

    public MatchResult match(String playerName) {
        if (playerName == null || playerName.isEmpty() || forbiddenPatterns.isEmpty()) {
            return MatchResult.noMatch();
        }
        Matcher matcher = pattern.matcher(playerName);
        if (!matcher.find()) {
            return MatchResult.noMatch();
        }
        return MatchResult.matched(playerName, matcher.group(), matcher.start(), matcher.end());
    }

    public MatchResult find(String playerName) {
        return match(playerName);
    }

    public boolean isForbidden(String playerName) {
        return match(playerName).isMatch();
    }

    public boolean containsForbiddenWord(String playerName) {
        return isForbidden(playerName);
    }

    public boolean test(String playerName) {
        return isForbidden(playerName);
    }

    public Pattern pattern() {
        return pattern;
    }

    public List<String> forbiddenPatterns() {
        return forbiddenPatterns;
    }

    public List<String> getForbiddenPatterns() {
        return forbiddenPatterns();
    }

    private static Pattern compile(List<String> patterns) {
        if (patterns.isEmpty()) {
            return NEVER_MATCH;
        }
        StringBuilder expression = new StringBuilder("(?:");
        for (int i = 0; i < patterns.size(); i++) {
            if (i > 0) {
                expression.append(")|(?:");
            }
            expression.append(patterns.get(i));
        }
        expression.append(')');
        return Pattern.compile(expression.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}
