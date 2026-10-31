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
        int count = Math.max(1, Math.min(200, limit));
        return CompletableFuture.supplyAsync(() -> searchBlocking(query, count), ioExecutor);
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

    private String searchBlocking(String query, int limit) {
        if (query == null || query.isBlank()) {
            return "Log search query is empty.";
        }
        try {
            String text = readLast(SEARCH_LIMIT);
            if (text == null) {
                return missing();
            }
            String needle = query.toLowerCase(java.util.Locale.ROOT);
            List<String> matches = new ArrayList<>();
            for (String line : text.split("\\R")) {
                if (line.toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                    matches.add(line);
                }
            }
            if (matches.isEmpty()) {
                return "No matching log lines in the last " + (SEARCH_LIMIT / 1024) + " KiB.";
            }
            int start = Math.max(0, matches.size() - limit);
            StringBuilder result = new StringBuilder();
            result.append("Matches: ")
                    .append(matches.size())
                    .append(", showing the newest ")
                    .append(matches.size() - start)
                    .append('\n');
            for (int index = start; index < matches.size(); index++) {
                result.append(matches.get(index)).append('\n');
            }
            return bounded(result.toString());
        } catch (IOException failure) {
            return "Could not search the client log: " + failure.getClass().getSimpleName();
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
