package com.izakji.mccreatorutils.client.agent.context;

import com.mojang.brigadier.tree.CommandNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;

/**
 * Lists client-visible server and modded commands.
 *
 * @author Izakji
 */
final class CommandCatalog {
    private static final int MAX_SCAN = 10_000;
    private static final int MAX_RESULTS = 500;
    private static final int MAX_DEPTH = 2;

    private CommandCatalog() {}

    static String describe(String query, int limit) {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        if (listener == null) {
            throw new IllegalStateException("No server connection is available.");
        }
        String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        int max = Math.max(1, Math.min(MAX_RESULTS, limit));
        Set<String> matches = new TreeSet<>();
        int[] scanned = {0};
        CommandNode<SharedSuggestionProvider> root = listener.getCommands().getRoot();
        for (CommandNode<SharedSuggestionProvider> child : sorted(root.getChildren())) {
            collect(child, "/" + child.getName(), 1, needle, matches, scanned);
            if (scanned[0] >= MAX_SCAN) {
                break;
            }
        }
        if (matches.isEmpty()) {
            return "No commands matched. Use an empty query to list commands.";
        }
        int shown = Math.min(max, matches.size());
        StringBuilder report = new StringBuilder();
        report.append("Commands (")
                .append(shown)
                .append(" of ")
                .append(matches.size())
                .append("):\n");
        int index = 0;
        for (String match : matches) {
            if (index++ >= shown) {
                break;
            }
            report.append("- ").append(match).append('\n');
        }
        if (shown < matches.size()) {
            report.append("Use query or a higher limit to see more.\n");
        }
        report.append("Server permissions still apply when running a command.");
        return report.toString();
    }

    private static void collect(
            CommandNode<SharedSuggestionProvider> node,
            String path,
            int depth,
            String query,
            Set<String> matches,
            int[] scanned) {
        if (scanned[0] >= MAX_SCAN) {
            return;
        }
        scanned[0]++;
        if (query.isEmpty() || path.toLowerCase(Locale.ROOT).contains(query)) {
            matches.add(path);
        }
        if (depth >= MAX_DEPTH) {
            return;
        }
        for (CommandNode<SharedSuggestionProvider> child : sorted(node.getChildren())) {
            collect(child, path + " " + child.getName(), depth + 1, query, matches, scanned);
            if (scanned[0] >= MAX_SCAN) {
                return;
            }
        }
    }

    private static List<CommandNode<SharedSuggestionProvider>> sorted(
            java.util.Collection<CommandNode<SharedSuggestionProvider>> children) {
        List<CommandNode<SharedSuggestionProvider>> values = new ArrayList<>(children);
        values.sort(Comparator.comparing(CommandNode::getName));
        return values;
    }
}
