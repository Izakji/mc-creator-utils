package com.izakji.agent.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;

final class FakeProcess extends Process {
    private final PipedOutputStream toProcess = new PipedOutputStream();
    private final PipedInputStream stdinReader;
    private final PipedInputStream fromProcess;
    private final PipedOutputStream stdoutWriter = new PipedOutputStream();
    private final PipedInputStream errorReader;
    private final PipedOutputStream errorWriter = new PipedOutputStream();
    private volatile boolean alive = true;

    private FakeProcess() throws IOException {
        stdinReader = new PipedInputStream(toProcess);
        fromProcess = new PipedInputStream(stdoutWriter);
        errorReader = new PipedInputStream(errorWriter);
    }

    static FakeProcess create() throws IOException {
        return new FakeProcess();
    }

    BufferedReader stdinReader() {
        return new BufferedReader(new InputStreamReader(stdinReader, StandardCharsets.UTF_8));
    }

    void writeStdout(String text) throws IOException {
        stdoutWriter.write(text.getBytes(StandardCharsets.UTF_8));
        stdoutWriter.flush();
    }

    void writeStderr(String text) throws IOException {
        errorWriter.write(text.getBytes(StandardCharsets.UTF_8));
        errorWriter.flush();
    }

    @Override
    public OutputStream getOutputStream() {
        return toProcess;
    }

    @Override
    public InputStream getInputStream() {
        return fromProcess;
    }

    @Override
    public InputStream getErrorStream() {
        return errorReader;
    }

    @Override
    public int waitFor() {
        return 0;
    }

    @Override
    public int exitValue() {
        return 0;
    }

    @Override
    public void destroy() {
        alive = false;
    }

    @Override
    public boolean isAlive() {
        return alive;
    }
}
