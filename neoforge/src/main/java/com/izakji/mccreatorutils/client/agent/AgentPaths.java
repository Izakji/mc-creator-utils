package com.izakji.mccreatorutils.client.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Filesystem layout for the client-side agent.
 *
 * @author Izakji
 */
public final class AgentPaths {
    private final Path root;

    private AgentPaths(Path root) {
        this.root = root;
    }

    /**
     * Creates paths under the Minecraft config directory.
     *
     * @param configDir Minecraft config directory
     * @return agent paths
     */
    public static AgentPaths at(Path configDir) {
        Path root = Objects.requireNonNull(configDir, "configDir")
                .resolve("mccreatorutils")
                .resolve("agent")
                .toAbsolutePath()
                .normalize();
        return new AgentPaths(root);
    }

    public Path root() {
        return root;
    }

    public Path settingsFile() {
        return root.resolve("agent.json");
    }

    public Path agentsMarkdown() {
        return root.resolve("AGENTS.md");
    }

    public Path skillsDir() {
        return root.resolve("skills");
    }

    public Path packsDir() {
        return root.resolve("plugins").resolve("packs");
    }

    public Path mcpFile() {
        return root.resolve("mcp.json");
    }

    public Path conversationsDir() {
        return root.resolve("conversations");
    }

    /**
     * Creates the writable agent directories.
     *
     * @throws IOException when a directory cannot be created
     */
    public void ensureDirectories() throws IOException {
        Files.createDirectories(skillsDir());
        Files.createDirectories(packsDir());
        Files.createDirectories(conversationsDir());
    }
}
