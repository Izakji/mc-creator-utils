package com.izakji.mccreatorutils.client.agent.context;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Bounded on-demand reader over the client's latest log.
 *
 * @author Izakji
 */
public final class ClientLogReader {
    private static final long TAIL_LIMIT = 256 * 1024;
    private static final long SEARCH_LIMIT = 1024 * 1024;
    private static final int MAX_OUTPUT = 32 * 1024;
    private final Path logFile;
    private final Executor ioExecutor;

    public ClientLogReader(Path logFile, Executor ioExecutor) {
        this.logFile = logFile;
        this.ioExecutor = ioExecutor;
    }

    public CompletableFuture<String> tail(int lines) {
        int count = Math.max(1, Math.min(500, lines));
        return CompletableFuture.supplyAsync(() -> tailBlocking(count), ioExecutor);
    }

    public CompletableFuture<String> search(String query, int limit) {
        return search(query, limit, "", 0);
    }

    public CompletableFuture<String> search(String query, int limit, String level, int context) {
        int count = Math.max(1, Math.min(200, limit));
        int surrounding = Math.max(0, Math.min(5, context));
        return CompletableFuture.supplyAsync(() -> searchBlocking(query, count, level, surrounding), ioExecutor);
    }

    public CompletableFuture<String> summary(int maxGroups) {
        int groups = Math.max(1, Math.min(50, maxGroups));
        return CompletableFuture.supplyAsync(() -> summaryBlocking(groups), ioExecutor);
    }

    public CompletableFuture<LogSummary> summaryData(int maxGroups) {
        int groups = Math.max(1, Math.min(50, maxGroups));
        return CompletableFuture.supplyAsync(() -> summaryDataBlocking(groups), ioExecutor);
    }

    private String tailBlocking(int lines) {
        try {
            String text = readLast(TAIL_LIMIT);
            if (text == null) {
                return missing();
            }
            String[] split = text.split("\\R");
            int start = Math.max(0, split.length - lines);
            StringBuilder result = new StringBuilder();
            for (int index = start; index < split.length; index++) {
                result.append(split[index]).append('\n');
            }
            return bounded(result.toString());
        } catch (IOException failure) {
            return "Could not read the client log: " + failure.getClass().getSimpleName();
        }
    }

    private String searchBlocking(String query, int limit, String level, int context) {
        if (query == null || query.isBlank()) {
            return "Log search query is empty.";
        }
        String normalizedLevel = LogSummaryReport.normalizedLevel(level);
        if (level != null && !level.isBlank() && normalizedLevel.isEmpty()) {
            return "Unknown log level: " + level;
        }
        try {
            String text = readLast(SEARCH_LIMIT);
            if (text == null) {
                return missing();
            }
            String[] lines = text.split("\\R", -1);
            String needle = query.toLowerCase(java.util.Locale.ROOT);
            List<Integer> matches = new ArrayList<>();
            for (int index = 0; index < lines.length; index++) {
                String line = lines[index];
                if (!line.toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                    continue;
                }
                if (!normalizedLevel.isEmpty() && !normalizedLevel.equals(LogSummaryReport.levelOf(line))) {
                    continue;
                }
                matches.add(index);
            }
            if (matches.isEmpty()) {
                return "No matching log lines in the last " + (SEARCH_LIMIT / 1024) + " KiB.";
            }
            int start = Math.max(0, matches.size() - limit);
            boolean[] selected = new boolean[lines.length];
            boolean[] matched = new boolean[lines.length];
            for (int index = start; index < matches.size(); index++) {
                int line = matches.get(index);
                matched[line] = true;
                int from = Math.max(0, line - context);
                int to = Math.min(lines.length - 1, line + context);
                for (int selectedLine = from; selectedLine <= to; selectedLine++) {
                    selected[selectedLine] = true;
                }
            }
            StringBuilder result = new StringBuilder();
            result.append("Matches: ")
                    .append(matches.size())
                    .append(", showing the newest ")
                    .append(matches.size() - start)
                    .append('\n');
            for (int index = 0; index < lines.length; index++) {
                if (selected[index]) {
                    result.append(matched[index] ? "> " : "  ")
                            .append(lines[index])
                            .append('\n');
                }
            }
            return bounded(result.toString());
        } catch (IOException failure) {
            return "Could not search the client log: " + failure.getClass().getSimpleName();
        }
    }

    private String summaryBlocking(int maxGroups) {
        try {
            String text = readLast(SEARCH_LIMIT);
            if (text == null) {
                return missing();
            }
            return LogSummaryReport.format(text, maxGroups);
        } catch (IOException failure) {
            return "Could not summarize the client log: " + failure.getClass().getSimpleName();
        }
    }

    private LogSummary summaryDataBlocking(int maxGroups) {
        try {
            String text = readLast(SEARCH_LIMIT);
            return LogSummary.parse(text == null ? "" : text, maxGroups);
        } catch (IOException failure) {
            return LogSummary.parse("", maxGroups);
        }
    }

    private String readLast(long maxBytes) throws IOException {
        if (!Files.isRegularFile(logFile)) {
            return null;
        }
        long size = Files.size(logFile);
        long start = Math.max(0, size - maxBytes);
        int capacity = (int) Math.min(Integer.MAX_VALUE, size - start);
        ByteBuffer buffer = ByteBuffer.allocate(capacity);
        try (SeekableByteChannel channel = Files.newByteChannel(logFile, StandardOpenOption.READ)) {
            channel.position(start);
            while (buffer.hasRemaining() && channel.read(buffer) > 0) {
                // Continue until the file tail is consumed.
            }
        }
        String text = new String(buffer.array(), 0, buffer.position(), StandardCharsets.UTF_8);
        if (start > 0) {
            int newline = text.indexOf('\n');
            if (newline >= 0) {
                text = text.substring(newline + 1);
            }
        }
        return text;
    }

    private String missing() {
        return "No client log found at " + logFile.toAbsolutePath() + ".";
    }

    private static String bounded(String text) {
        return text.length() <= MAX_OUTPUT ? text : text.substring(text.length() - MAX_OUTPUT);
    }
}
