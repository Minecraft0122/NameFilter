package com.namefilter.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NameFilterConfigParserTest {
    @Test
    void parsesPlatformFriendlyMap() {
        NameFilterConfig config = NameFilterConfigParser.parse(Map.of(
                "handling-mode", "silent",
                "kickMessage", "Blocked"
        ));

        assertEquals(HandlingMode.SILENT, config.handlingMode());
        assertEquals("Blocked", config.rejectionMessage());
    }

    @Test
    void parsesDefaults() {
        NameFilterConfig config = NameFilterConfigParser.parse(Map.of());

        assertEquals(HandlingMode.MESSAGE, config.handlingMode());
        assertEquals(NameFilterConfig.DEFAULT_REJECTION_MESSAGE, config.rejectionMessage());
    }
}
