package com.namefilter.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameMatcherTest {
    @Test
    void findsLiteralSubstringCaseInsensitively() {
        NameMatcher matcher = new NameMatcher(List.of("admin"));

        MatchResult result = matcher.match("BestADMINPlayer");

        assertTrue(result.isMatch());
        assertEquals("ADMIN", result.matchedText());
        assertEquals(4, result.start());
        assertEquals(9, result.end());
    }

    @Test
    void evaluatesEntriesAsRegexFragments() {
        NameMatcher matcher = new NameMatcher(List.of("a.b", "\\[staff\\]"));

        assertTrue(matcher.isForbidden("xxAxBxx"));
        assertTrue(matcher.isForbidden("xx[STAFF]xx"));
        assertFalse(matcher.isForbidden("ab"));
    }

    @Test
    void builtInListContainsLeaderNamesInUsableUsernameForm() {
        NameMatcher matcher = NameMatcher.defaults();

        assertTrue(matcher.isForbidden("xxXiJinpingxx"));
        assertTrue(matcher.isForbidden("xxXi_Jin_Pingxx"));
        assertTrue(matcher.isForbidden("PlayerTrump"));
        assertFalse(matcher.isForbidden("RegularPlayer"));
    }

    @Test
    void builtInListContainsAdditionalRomanizedInsults() {
        NameFilterConfig config = NameFilterConfig.builder()
                .enforceNamePattern(false)
                .build();
        NameMatcher matcher = new NameMatcher(config, BuiltInForbiddenPatterns.PATTERNS);

        assertTrue(matcher.isForbidden("ruozhi"));
        assertTrue(matcher.isForbidden("RUO_ZHI"));
        assertTrue(matcher.isForbidden("caosiNMquanjia"));
        assertTrue(matcher.isForbidden("CAO_SI_N_M_QUAN_JIA"));
        assertTrue(matcher.isForbidden("ruo---zhi"));
    }

    @Test
    void builtInListContainsAdditionalEnglishAndChineseInsults() {
        NameFilterConfig config = NameFilterConfig.builder()
                .enforceNamePattern(false)
                .build();
        NameMatcher matcher = new NameMatcher(config, BuiltInForbiddenPatterns.PATTERNS);

        assertTrue(matcher.isForbidden("i.d.i.o.t"));
        assertTrue(matcher.isForbidden("PlayerAsshole"));
        assertTrue(matcher.isForbidden("Bai_Chi"));
        assertTrue(matcher.isForbidden("ben-dan"));
        assertTrue(matcher.isForbidden("cao-ni-ma"));
        assertTrue(matcher.isForbidden("wcnmPlayer"));
    }

    @Test
    void defaultMatcherRejectsCharactersOutsideTheUsernamePattern() {
        NameMatcher matcher = NameMatcher.defaults();

        assertFalse(matcher.isForbidden("Player_123"));
        assertTrue(matcher.isForbidden("Player-123"));
        assertEquals("-", matcher.match("Player-123").matchedText());
        assertTrue(matcher.isForbidden("玩家"));
        assertEquals("😀", matcher.match("Player😀").matchedText());
    }

    @Test
    void namePatternCanBeDisabledWithoutDisablingForbiddenWords() {
        NameFilterConfig config = NameFilterConfig.builder()
                .enforceNamePattern(false)
                .build();
        NameMatcher matcher = new NameMatcher(config);

        assertFalse(matcher.isForbidden("Player-123"));
        assertTrue(matcher.isForbidden("PlayerTrump"));
    }

    @Test
    void emptyListNeverMatches() {
        NameMatcher matcher = new NameMatcher(List.of());

        assertFalse(matcher.isForbidden("anything"));
        assertFalse(matcher.match(null).isMatch());
    }
}
