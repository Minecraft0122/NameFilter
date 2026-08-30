package com.namefilter.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Loads the editable forbidden-pattern file once during plugin startup. */
public final class ForbiddenPatternLoader {
    public static final String FILE_NAME = "forbidden-patterns.txt";
    private static final int REGEX_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    private ForbiddenPatternLoader() {
    }

    /**
     * Ensure the editable file exists and load its patterns into memory.
     *
     * <p>The returned list is detached from the file. Callers should retain it
     * and reuse it instead of invoking this method during individual login
     * checks.</p>
     */
    public static List<String> load(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        Path normalized = file.toAbsolutePath().normalize();
        Path parent = normalized.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        createDefaultFileIfMissing(normalized);
        return readPatterns(normalized);
    }

    /** Load the bundled defaults once for compatibility APIs and unit tests. */
    static List<String> loadBundled() {
        try (InputStream input = openBundledResource()) {
            return parse(input, "bundled " + FILE_NAME);
        } catch (IOException error) {
            throw new IllegalStateException(
                    "Could not load bundled " + FILE_NAME + ": " + error.getMessage(),
                    error);
        }
    }

    private static void createDefaultFileIfMissing(Path file) throws IOException {
        if (Files.exists(file)) {
            return;
        }
        try (InputStream input = openBundledResource();
                OutputStream output = Files.newOutputStream(
                        file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            input.transferTo(output);
        } catch (FileAlreadyExistsException ignored) {
            // Another startup task created it between the existence check and
            // CREATE_NEW; load whichever file is now present.
        }
    }

    private static List<String> readPatterns(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(reader, file.toString());
        }
    }

    private static InputStream openBundledResource() throws IOException {
        InputStream input = ForbiddenPatternLoader.class.getResourceAsStream(
                "/" + FILE_NAME);
        if (input == null) {
            throw new IOException("Missing bundled resource " + FILE_NAME);
        }
        return input;
    }

    private static List<String> parse(InputStream input, String source) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return parse(reader, source);
        }
    }

    private static List<String> parse(BufferedReader reader, String source) throws IOException {
        Set<String> patterns = new LinkedHashSet<>();
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (lineNumber == 1 && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
                line = line.substring(1);
            }
            String pattern = line.trim();
            if (!pattern.isEmpty() && !pattern.startsWith("#")) {
                validatePattern(pattern, source, lineNumber);
                patterns.add(pattern);
            }
        }
        validateCombinedPattern(patterns, source);
        return List.copyOf(patterns);
    }

    private static void validatePattern(String pattern, String source, int lineNumber)
            throws IOException {
        try {
            Pattern.compile(pattern, REGEX_FLAGS);
        } catch (PatternSyntaxException error) {
            throw new IOException(
                    "Invalid regular-expression fragment in " + source
                            + " at line " + lineNumber + ": " + error.getDescription(),
                    error);
        }
    }

    private static void validateCombinedPattern(Set<String> patterns, String source)
            throws IOException {
        if (patterns.isEmpty()) {
            return;
        }
        StringBuilder expression = new StringBuilder("(?:");
        int index = 0;
        for (String pattern : patterns) {
            if (index++ > 0) {
                expression.append(")|(?:");
            }
            expression.append(pattern);
        }
        expression.append(')');
        try {
            Pattern.compile(expression.toString(), REGEX_FLAGS);
        } catch (PatternSyntaxException error) {
            throw new IOException(
                    "Invalid combined regular-expression in " + source + ": "
                            + error.getDescription(),
                    error);
        }
    }
}
