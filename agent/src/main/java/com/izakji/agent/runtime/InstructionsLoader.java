package com.izakji.agent.runtime;

import com.izakji.agent.api.AgentException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads the persistent AGENTS.md instruction file.
 *
 * @author Izakji
 */
public final class InstructionsLoader {
    private static final int MAX_BYTES = 32 * 1024;

    public String load(Path file) throws AgentException {
        if (file == null || !Files.isRegularFile(file)) {
            return "";
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            return text.length() <= MAX_BYTES ? text : text.substring(0, MAX_BYTES);
        } catch (IOException failure) {
            throw new AgentException("Could not read agent instructions", failure);
        }
    }
}
