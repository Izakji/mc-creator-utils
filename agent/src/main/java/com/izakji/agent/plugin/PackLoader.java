package com.izakji.agent.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.izakji.agent.mcp.McpServerConfig;
import com.izakji.agent.skill.Skill;
import com.izakji.agent.skill.SkillLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Loads declarative packs without executing code.
 *
 * @author Izakji
 */
public final class PackLoader {
    private final Gson gson = new Gson();

    public PackLoadResult load(Path packsRoot) {
        if (packsRoot == null || !Files.isDirectory(packsRoot)) {
            return new PackLoadResult(List.of(), List.of());
        }
        List<PluginPack> packs = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try (Stream<Path> dirs = Files.list(packsRoot)) {
            dirs.filter(Files::isDirectory).forEach(dir -> {
                try {
                    PluginPack pack = loadPack(dir);
                    if (pack != null) {
                        packs.add(pack);
                    }
                } catch (RuntimeException | IOException failure) {
                    errors.add(dir.getFileName() + ": " + failure.getMessage());
                }
            });
        } catch (IOException failure) {
            errors.add("packs: " + failure.getMessage());
        }
        return new PackLoadResult(packs, errors);
    }

    private PluginPack loadPack(Path dir) throws IOException {
        Path manifest = dir.resolve("pack.json");
        if (!Files.isRegularFile(manifest)) {
            return null;
        }
        JsonObject root = JsonParser.parseString(Files.readString(manifest)).getAsJsonObject();
        String id = required(root, "id");
        String name = root.has("name") ? root.get("name").getAsString() : id;
        String version = root.has("version") ? root.get("version").getAsString() : "";
        List<Skill> skills = new ArrayList<>();
        SkillLoader skillLoader = new SkillLoader();
        for (JsonElement element : array(root, "skills")) {
            Path resolved = resolveInside(dir, element.getAsString());
            skillLoader.loadOne(resolved).ifPresent(skills::add);
        }
        List<McpServerConfig> servers = new ArrayList<>();
        for (JsonElement element : array(root, "mcpServers")) {
            Path resolved = resolveInside(dir, element.getAsString());
            McpServerConfig[] loaded = gson.fromJson(Files.readString(resolved), McpServerConfig[].class);
            if (loaded != null) {
                servers.addAll(List.of(loaded));
            }
        }
        return new PluginPack(id, name, version, skills, servers);
    }

    private static Path resolveInside(Path root, String relative) {
        Path base = root.toAbsolutePath().normalize();
        Path resolved = base.resolve(relative).normalize();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("path escapes pack: " + relative);
        }
        return resolved;
    }

    private static String required(JsonObject object, String name) {
        if (!object.has(name) || object.get(name).isJsonNull()) {
            throw new IllegalArgumentException("missing " + name);
        }
        return object.get(name).getAsString();
    }

    private static JsonArray array(JsonObject object, String name) {
        if (!object.has(name)) {
            return new JsonArray();
        }
        return object.getAsJsonArray(name);
    }
}
