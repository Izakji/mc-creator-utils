package com.izakji.mccreatorutils.client.agent.context;

import com.google.gson.JsonObject;
import java.util.Locale;

/**
 * Classifies commands that are safe to auto-run.
 *
 * @author Izakji
 */
final class CommandSafety {
    private CommandSafety() {}

    static String command(JsonObject arguments) {
        if (arguments == null || !arguments.has("command")) {
            return "";
        }
        return arguments.get("command").getAsString();
    }

    static boolean isReadOnly(JsonObject arguments) {
        return isReadOnly(command(arguments));
    }

    static boolean isReadOnly(String raw) {
        String[] tokens = tokens(raw);
        if (tokens.length == 0) {
            return false;
        }
        String root = tokens[0];
        if (root.equals("locate") || root.equals("seed") || root.equals("help") || root.equals("list")) {
            return true;
        }
        if (root.equals("time") && tokens.length > 1 && tokens[1].equals("query")) {
            return true;
        }
        if (root.equals("data") && tokens.length > 1 && tokens[1].equals("get")) {
            return true;
        }
        if (root.equals("worldborder") && tokens.length > 1 && tokens[1].equals("get")) {
            return true;
        }
        if (root.equals("xp") && tokens.length > 1 && tokens[1].equals("query")) {
            return true;
        }
        if (root.equals("difficulty") && tokens.length == 1) {
            return true;
        }
        if (root.equals("gamerule") && tokens.length == 2) {
            return true;
        }
        if (root.equals("scoreboard") && tokens.length > 2) {
            boolean players = tokens[1].equals("players")
                    && (tokens[2].equals("get") || (tokens[2].equals("list") && tokens.length == 3));
            boolean objectives = tokens[1].equals("objectives") && tokens[2].equals("list");
            return players || objectives;
        }
        return false;
    }

    static String[] tokens(String raw) {
        String value = raw == null ? "" : raw.strip();
        while (value.startsWith("/")) {
            value = value.substring(1).strip();
        }
        if (value.isEmpty()) {
            return new String[0];
        }
        String[] parts = value.split("\\s+");
        String[] tokens = new String[parts.length];
        for (int index = 0; index < parts.length; index++) {
            tokens[index] = parts[index].toLowerCase(Locale.ROOT);
        }
        return tokens;
    }
}
