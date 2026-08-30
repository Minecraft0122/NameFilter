package com.namefilter.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForbiddenPatternLoaderTest {
    @Test
    void createsAndLoadsEditableFileWithBuiltInPatterns(@TempDir Path directory)
            throws Exception {
        Path file = directory.resolve(ForbiddenPatternLoader.FILE_NAME);

        List<String> patterns = ForbiddenPatternLoader.load(file);

        assertTrue(Files.exists(file));
        assertEquals(BuiltInForbiddenPatterns.PATTERNS, patterns);
        assertThrows(UnsupportedOperationException.class, () -> patterns.add("another"));
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains(
                "Edit this file in the plugin data directory"));
    }

    @Test
    void ignoresBlankCommentLinesAndBom(@TempDir Path directory)
            throws Exception {
        Path file = directory.resolve(ForbiddenPatternLoader.FILE_NAME);
        Files.writeString(file, "\uFEFF\n# comment\n  custom-one  \n\ncustom-two\n", StandardCharsets.UTF_8);

        assertEquals(List.of("custom-one", "custom-two"), ForbiddenPatternLoader.load(file));
    }

    @Test
    void matcherKeepsLoadedPatternsAndNamePolicyAfterFileChanges(
            @TempDir Path directory)
            throws Exception {
        Path file = directory.resolve(ForbiddenPatternLoader.FILE_NAME);
        Files.writeString(file, "custom\n", StandardCharsets.UTF_8);
        NameMatcher matcher = new NameMatcher(NameFilterConfig.defaults(),
                ForbiddenPatternLoader.load(file));

        Files.delete(file);

        assertTrue(matcher.isForbidden("customPlayer"));
        assertFalse(matcher.isForbidden("changedPlayer"));
        assertTrue(matcher.isForbidden("changed-player"));
    }

    @Test
    void emptyFileDisablesForbiddenPatternsButStillLoadsSuccessfully(
            @TempDir Path directory) throws Exception {
        Path file = directory.resolve(ForbiddenPatternLoader.FILE_NAME);
        Files.writeString(file, "# intentionally empty\n", StandardCharsets.UTF_8);

        assertEquals(List.of(), ForbiddenPatternLoader.load(file));
    }

    @Test
    void invalidRegexReportsTheFileAndLine(
            @TempDir Path directory) throws Exception {
        Path file = directory.resolve(ForbiddenPatternLoader.FILE_NAME);
        Files.writeString(file, "valid\n[broken\n", StandardCharsets.UTF_8);

        IOException error = assertThrows(IOException.class, () -> ForbiddenPatternLoader.load(file));

        assertTrue(error.getMessage().contains("line 2"));
        assertTrue(error.getMessage().contains("Invalid regular-expression fragment"));
    }

    @Test
    void doesNotOverwriteAnExistingUserFile(@TempDir Path directory)
            throws Exception {
        Path file = directory.resolve(ForbiddenPatternLoader.FILE_NAME);
        Files.writeString(file, "custom\n", StandardCharsets.UTF_8);

        assertEquals(List.of("custom"), ForbiddenPatternLoader.load(file));
        assertEquals("custom\n", Files.readString(file, StandardCharsets.UTF_8));
    }
}
