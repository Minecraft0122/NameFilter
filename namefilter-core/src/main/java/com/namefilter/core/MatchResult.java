package com.namefilter.core;

import java.util.Optional;

/** The result of checking one player name. */
public final class MatchResult {
    private static final MatchResult NO_MATCH = new MatchResult(null, null, -1, -1);

    private final String playerName;
    private final String matchedText;
    private final int start;
    private final int end;

    private MatchResult(String playerName, String matchedText, int start, int end) {
        this.playerName = playerName;
        this.matchedText = matchedText;
        this.start = start;
        this.end = end;
    }

    static MatchResult noMatch() {
        return NO_MATCH;
    }

    static MatchResult matched(String playerName, String matchedText, int start, int end) {
        return new MatchResult(playerName, matchedText, start, end);
    }

    public boolean isMatch() {
        return matchedText != null;
    }

    public boolean matched() {
        return isMatch();
    }

    public boolean isForbidden() {
        return isMatch();
    }

    public String playerName() {
        return playerName;
    }

    public String getPlayerName() {
        return playerName();
    }

    /** The text as it occurred in the player name, or {@code null} when absent. */
    public String matchedText() {
        return matchedText;
    }

    public String getMatchedText() {
        return matchedText();
    }

    public Optional<String> matchedTextOptional() {
        return Optional.ofNullable(matchedText);
    }

    public int start() {
        return start;
    }

    public int getStart() {
        return start();
    }

    public int end() {
        return end;
    }

    public int getEnd() {
        return end();
    }

    @Override
    public String toString() {
        if (!isMatch()) {
            return "MatchResult{matched=false}";
        }
        return "MatchResult{matched=true, playerName='" + playerName
                + "', matchedText='" + matchedText + "', start=" + start
                + ", end=" + end + "}";
    }
}
