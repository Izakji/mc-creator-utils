package com.izakji.agent.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.izakji.agent.api.AgentException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Loads and saves {@link AgentSettings} as JSON.
 *
 * @author Izakji
 */
public final class AgentSettingsStore {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public AgentSettings load(Path file) throws AgentException {
        try {
            AgentSettings settings = gson.fromJson(Files.readString(file), AgentSettings.class);
            if (settings == null) {
                throw new AgentException("Agent settings are empty");
            }
            return settings;
        } catch (IOException | RuntimeException failure) {
            throw new AgentException("Could not read agent settings", failure);
        }
    }

    public void save(Path file, AgentSettings settings) throws AgentException {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
            Files.writeString(temp, gson.toJson(settings));
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicFailure) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException failure) {
            throw new AgentException("Could not save agent settings", failure);
        }
    }
}
