package com.izakji.mccreatorutils.client.agent.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Formats installed mod rows for the agent.
 *
 * @author Izakji
 */
public final class ModListReport {
    private ModListReport() {}

    public static String format(List<ModEntry> mods, String query, int limit) {
        String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        List<ModEntry> matching = new ArrayList<>();
        for (ModEntry mod : mods) {
            if (needle.isEmpty()
                    || mod.id().toLowerCase(Locale.ROOT).contains(needle)
                    || mod.name().toLowerCase(Locale.ROOT).contains(needle)
                    || mod.authors().toLowerCase(Locale.ROOT).contains(needle)) {
                matching.add(mod);
            }
        }
        if (matching.isEmpty()) {
            return "No installed mods matched.";
        }
        int shown = Math.min(Math.max(1, limit), matching.size());
        StringBuilder report = new StringBuilder();
        report.append("Installed mods (")
                .append(shown)
                .append(" of ")
                .append(matching.size())
                .append("):\n");
        for (int index = 0; index < shown; index++) {
            ModEntry mod = matching.get(index);
            report.append("- ")
                    .append(mod.id())
                    .append(" | ")
                    .append(mod.name())
                    .append(" | ")
                    .append(mod.version());
            if (!mod.authors().isBlank()) {
                report.append(" | ").append(mod.authors());
            }
            report.append('\n');
        }
        if (shown < matching.size()) {
            report.append("Use query or a higher limit to see more.\n");
        }
        return report.toString();
    }
}
