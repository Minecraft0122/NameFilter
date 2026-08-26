package com.namefilter.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameFilterConfigParserTest {
    @Test
    void parsesPlatformFriendlyMap() {
        NameFilterConfig config = NameFilterConfigParser.parse(Map.of(
                "handling-mode", "silent",
                "kickMessage", "Blocked"
        ));

        assertEquals(HandlingMode.SILENT, config.handlingMode());
        assertEquals("Blocked", config.rejectionMessage());
        assertTrue(config.enforceNamePattern());
    }

    @Test
    void parsesDefaults() {
        NameFilterConfig config = NameFilterConfigParser.parse(Map.of());

        assertEquals(HandlingMode.MESSAGE, config.handlingMode());
        assertEquals(NameFilterConfig.DEFAULT_REJECTION_MESSAGE, config.rejectionMessage());
        assertTrue(config.enforceNamePattern());
    }

    @Test
    void canDisableNamePatternWithCanonicalKey() {
        NameFilterConfig config = NameFilterConfigParser.parse(Map.of(
                "enforce-name-pattern", "false"
        ));

        assertFalse(config.enforceNamePattern());
    }

    @Test
    void acceptsBooleanValuesAndCharacterValidationAlias() {
        NameFilterConfig config = NameFilterConfigParser.parse(Map.of(
                "validate-name-characters", false
        ));

        assertFalse(config.enforceNamePattern());
        assertFalse(config.validateNameCharacters());
    }

    @Test
    void invalidNamePatternValueRetainsSecureDefault() {
        NameFilterConfig config = NameFilterConfigParser.parse(Map.of(
                "enforce-name-pattern", "sometimes"
        ));

        assertTrue(config.enforceNamePattern());
    }
}
