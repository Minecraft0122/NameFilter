package com.namefilter.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Non-blocking audit writer which appends rejected joins to
 * {@code log/yyyy-MM-dd.log}.
 *
 * <p>A single background daemon writes entries in order. The bounded queue
 * prevents a connection flood from growing memory without limit. Callers can
 * observe an overload or I/O failure through the returned completion stage.</p>
 */
public final class DailyAuditLogger implements AutoCloseable {
    private static final int DEFAULT_QUEUE_CAPACITY = 8_192;
    private static final Duration DEFAULT_CLOSE_TIMEOUT = Duration.ofSeconds(5);
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ENTRY_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final Path logDirectory;
    private final ZoneId zoneId;
    private final ThreadPoolExecutor executor;
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * @param logDirectory the exact directory that will contain daily log files
     */
    public DailyAuditLogger(Path logDirectory) {
        this(logDirectory, ZoneId.systemDefault(), DEFAULT_QUEUE_CAPACITY);
    }

    public DailyAuditLogger(Path logDirectory, ZoneId zoneId) {
        this(logDirectory, zoneId, DEFAULT_QUEUE_CAPACITY);
    }

    DailyAuditLogger(Path logDirectory, ZoneId zoneId, int queueCapacity) {
        this.logDirectory = Objects.requireNonNull(logDirectory, "logDirectory")
                .toAbsolutePath().normalize();
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
        if (queueCapacity < 1) {
            throw new IllegalArgumentException("queueCapacity must be positive");
        }
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "NameFilter-Audit");
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((ignored, error) -> error.printStackTrace(System.err));
            return thread;
        };
        this.executor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity), threadFactory,
                new ThreadPoolExecutor.AbortPolicy());
    }

    /** Queue a record without doing file I/O on the connection event thread. */
    public CompletionStage<Void> record(AuditRecord record) {
        Objects.requireNonNull(record, "record");
        CompletableFuture<Void> completion = new CompletableFuture<>();
        if (closed.get()) {
            completion.completeExceptionally(new IllegalStateException("Audit logger is closed"));
            return completion;
        }

        try {
            executor.execute(() -> {
                try {
                    append(record);
                    completion.complete(null);
                } catch (Throwable error) {
                    completion.completeExceptionally(error);
                }
            });
        } catch (RejectedExecutionException error) {
            completion.completeExceptionally(error);
        }
        return completion;
    }

    public CompletionStage<Void> record(
            String playerName,
            MatchResult match,
            String platform,
            HandlingMode handlingMode
    ) {
        return record(AuditRecord.blocked(platform, playerName, null, match, handlingMode));
    }

    public CompletionStage<Void> record(
            String playerName,
            String remoteAddress,
            MatchResult match,
            String platform,
            HandlingMode handlingMode
    ) {
        return record(AuditRecord.blocked(
                platform, playerName, remoteAddress, match, handlingMode));
    }

    /** Alias useful for adapters that treat the logger as a simple sink. */
    public CompletionStage<Void> log(AuditRecord record) {
        return record(record);
    }

    public Path logDirectory() {
        return logDirectory;
    }

    private void append(AuditRecord record) throws IOException {
        Files.createDirectories(logDirectory);
        String date = FILE_DATE.format(record.timestamp().atZone(zoneId));
        Path file = logDirectory.resolve(date + ".log");
        String line = format(record) + System.lineSeparator();
        Files.writeString(
                file,
                line,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
    }

    String format(AuditRecord record) {
        return "[" + ENTRY_TIME.format(record.timestamp().atZone(zoneId)) + "]"
                + " platform=" + quote(record.platform())
                + " player=" + quote(record.playerName())
                + " remote=" + quote(record.remoteAddress())
                + " matched=" + quote(record.matchedText())
                + " mode=" + record.handlingMode().name();
    }

    private static String quote(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\r' -> escaped.append("\\r");
                case '\n' -> escaped.append("\\n");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(character);
            }
        }
        return escaped.append('"').toString();
    }

    /** Flush queued entries and stop the worker. Safe to call more than once. */
    @Override
    public void close() {
        close(DEFAULT_CLOSE_TIMEOUT);
    }

    public boolean close(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (!closed.compareAndSet(false, true)) {
            return executor.isTerminated();
        }
        executor.shutdown();
        try {
            if (executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                return true;
            }
            executor.shutdownNow();
            return false;
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
