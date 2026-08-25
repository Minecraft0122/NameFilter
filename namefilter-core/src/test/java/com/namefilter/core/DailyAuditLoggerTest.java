package com.namefilter.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyAuditLoggerTest {
    @TempDir
    Path tempDirectory;

    @Test
    void writesToExpectedDailyFileAndEscapesNewlines() throws Exception {
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        DailyAuditLogger logger = new DailyAuditLogger(tempDirectory.resolve("log"), zone);
        AuditRecord record = new AuditRecord(
                Instant.parse("2026-08-24T14:00:00Z"),
                "Paper",
                "Bad\nName",
                "127.0.0.1:25565",
                "Bad",
                HandlingMode.MESSAGE);

        logger.record(record).toCompletableFuture().get(5, TimeUnit.SECONDS);
        logger.close();

        Path logFile = tempDirectory.resolve("log/2026-08-24.log");
        String content = Files.readString(logFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("platform=\"Paper\""));
        assertTrue(content.contains("player=\"Bad\\nName\""));
        assertTrue(content.contains("matched=\"Bad\""));
        assertTrue(content.contains("mode=MESSAGE"));
    }
}
