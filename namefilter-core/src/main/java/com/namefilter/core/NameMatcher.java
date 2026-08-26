package com.namefilter.core;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fast, reusable matcher for player names.
 *
 * <p>All built-in regex fragments and the optional username character policy
 * are compiled once, while each check only allocates the JDK's lightweight
 * {@link Matcher} object(s).</p>
 */
public final class NameMatcher {
    private static final Pattern NEVER_MATCH = Pattern.compile("(?!x)x");
    private static final Pattern DEFAULT_PATTERN = compile(BuiltInForbiddenPatterns.PATTERNS);
    private static final Pattern VALID_NAME_PATTERN = Pattern.compile(
            NameFilterConfig.DEFAULT_NAME_PATTERN);

    private final Pattern pattern;
    private final List<String> forbiddenPatterns;
    private final boolean enforceNamePattern;

    public NameMatcher(NameFilterConfig config) {
        Objects.requireNonNull(config, "config");
        this.forbiddenPatterns = BuiltInForbiddenPatterns.PATTERNS;
        this.pattern = DEFAULT_PATTERN;
        this.enforceNamePattern = config.enforceNamePattern();
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
        this.enforceNamePattern = false;
    }

    public static NameMatcher defaults() {
        return new NameMatcher(NameFilterConfig.defaults());
    }

    /** The compiled built-in forbidden-word regex, useful for diagnostics/metrics. */
    public static Pattern defaultPattern() {
        return DEFAULT_PATTERN;
    }

    public MatchResult match(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return MatchResult.noMatch();
        }

        if (enforceNamePattern && !VALID_NAME_PATTERN.matcher(playerName).matches()) {
            int invalidStart = firstInvalidCharacterStart(playerName);
            if (invalidStart >= 0) {
                int invalidEnd = invalidStart + Character.charCount(
                        playerName.codePointAt(invalidStart));
                return MatchResult.matched(
                        playerName,
                        playerName.substring(invalidStart, invalidEnd),
                        invalidStart,
                        invalidEnd);
            }
            // The validation and diagnostic patterns intentionally share the
            // same character policy; this is only a defensive fallback.
            return MatchResult.matched(playerName, playerName, 0, playerName.length());
        }

        if (forbiddenPatterns.isEmpty()) {
            return MatchResult.noMatch();
        }
        Matcher matcher = pattern.matcher(playerName);
        if (!matcher.find()) {
            return MatchResult.noMatch();
        }
        return MatchResult.matched(playerName, matcher.group(), matcher.start(), matcher.end());
    }

    private static int firstInvalidCharacterStart(String playerName) {
        for (int offset = 0; offset < playerName.length();) {
            int codePoint = playerName.codePointAt(offset);
            if (!isAllowedCharacter(codePoint)) {
                return offset;
            }
            offset += Character.charCount(codePoint);
        }
        return -1;
    }

    private static boolean isAllowedCharacter(int codePoint) {
        return (codePoint >= 'a' && codePoint <= 'z')
                || (codePoint >= 'A' && codePoint <= 'Z')
                || (codePoint >= '0' && codePoint <= '9')
                || codePoint == '_';
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
