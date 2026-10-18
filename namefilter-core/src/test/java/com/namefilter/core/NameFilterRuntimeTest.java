package com.namefilter.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NameFilterRuntimeTest {
    @Test
    void reloadPublishesNewMatcherOnlyAfterSuccessfulLoad(@TempDir Path directory) throws Exception {
        Path file = directory.resolve(ForbiddenPatternLoader.FILE_NAME);
        Files.writeString(file, "first\n", StandardCharsets.UTF_8);
        NameFilterRuntime runtime = new NameFilterRuntime(
                NameFilterConfig.defaults(), ForbiddenPatternLoader.load(file));
        NameFilterRuntime.Snapshot original = runtime.snapshot();

        Files.writeString(file, "second\n", StandardCharsets.UTF_8);
        runtime.reload(NameFilterConfig.defaults(), file);

        NameFilterRuntime.Snapshot updated = runtime.snapshot();
        assertNotSame(original, updated);
        assertTrue(updated.matcher().isForbidden("secondPlayer"));
        assertFalse(updated.matcher().isForbidden("firstPlayer"));

        Files.writeString(file, "[invalid\n", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> runtime.reload(NameFilterConfig.defaults(), file));
        assertSame(updated, runtime.snapshot());
        assertTrue(runtime.snapshot().matcher().isForbidden("secondPlayer"));
    }

    @Test
    void reloadUpdatesConfigAndMatcherTogether(@TempDir Path directory) throws Exception {
        Path file = directory.resolve(ForbiddenPatternLoader.FILE_NAME);
        Files.writeString(file, "custom\n", StandardCharsets.UTF_8);
        NameFilterRuntime runtime = new NameFilterRuntime(NameFilterConfig.defaults(), List.of("initial"));
        NameFilterConfig updatedConfig = NameFilterConfig.builder()
                .enforceNamePattern(false)
                .rejectionMessage("Custom rejection")
                .build();

        runtime.reload(updatedConfig, file);

        assertTrue(runtime.snapshot().matcher().isForbidden("customPlayer"));
        assertFalse(runtime.snapshot().matcher().isForbidden("initialPlayer"));
        assertFalse(runtime.snapshot().config().enforceNamePattern());
        assertTrue(runtime.snapshot().config().rejectionMessage().equals("Custom rejection"));
    }
}
