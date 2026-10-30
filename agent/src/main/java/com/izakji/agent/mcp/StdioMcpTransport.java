package com.izakji.agent.mcp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Newline-delimited JSON-RPC over a child process.
 *
 * @author Izakji
 */
public final class StdioMcpTransport implements McpTransport {
    private final McpServerConfig config;
    private final ProcessFactory factory;
    private final Deque<String> stderr = new ArrayDeque<>();
    private Process process;
    private Writer stdin;
    private Consumer<JsonObject> listener = ignored -> {};

    public StdioMcpTransport(McpServerConfig config, ProcessFactory factory) {
        this.config = config;
        this.factory = factory;
    }

    @Override
    public CompletableFuture<Void> start(Consumer<JsonObject> listener) {
        this.listener = listener;
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(config.command() == null ? "node" : config.command());
            builder.command().addAll(config.args());
            if (config.workingDirectory() != null && !config.workingDirectory().isBlank()) {
                builder.directory(Path.of(config.workingDirectory()).toFile());
            }
            Map<String, String> environment = builder.environment();
            environment.putAll(config.environment());
            process = factory.start(builder);
            stdin = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8);
            Thread reader = new Thread(this::readStdout, "mcp-" + config.id() + "-stdout");
            reader.setDaemon(true);
            reader.start();
            Thread errors = new Thread(this::readStderr, "mcp-" + config.id() + "-stderr");
            errors.setDaemon(true);
            errors.start();
            return CompletableFuture.completedFuture(null);
        } catch (IOException failure) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Could not start MCP server " + config.id(), failure));
        }
    }

    @Override
    public void send(JsonObject message) {
        if (stdin == null) {
            throw new IllegalStateException("MCP server is not started");
        }
        try {
            synchronized (this) {
                stdin.write(message.toString());
                stdin.write('\n');
                stdin.flush();
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Could not write to MCP server " + config.id(), failure);
        }
    }

    @Override
    public void close() {
        if (process != null) {
            process.destroy();
        }
        try {
            if (stdin != null) {
                stdin.close();
            }
        } catch (IOException ignored) {
            // Process teardown is best effort.
        }
    }

    private void readStdout() {
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    try {
                        listener.accept(JsonParser.parseString(line).getAsJsonObject());
                    } catch (RuntimeException ignored) {
                        // One malformed server message must not stop the reader.
                    }
                }
            }
        } catch (IOException ignored) {
            // The client reports a closed transport through request timeouts.
        }
    }

    private void readStderr() {
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                synchronized (stderr) {
                    stderr.addLast(line);
                    while (stderr.size() > 50) {
                        stderr.removeFirst();
                    }
                }
            }
        } catch (IOException ignored) {
            // Diagnostics are optional.
        }
    }
}
