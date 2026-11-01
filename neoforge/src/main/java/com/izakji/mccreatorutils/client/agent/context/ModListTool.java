package com.izakji.mccreatorutils.client.agent.context;

import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * Read-only installed-mod listing with a short cache.
 *
 * @author Izakji
 */
public final class ModListTool implements AgentTool {
    private static final Set<String> SYSTEM_MODS = Set.of("minecraft", "neoforge", "fml");
    private static final long CACHE_MILLIS = 60_000;
    private List<ModEntry> cache = List.of();
    private long cachedAt;

    @Override
    public String name() {
        return "list_mods";
    }

    @Override
    public String description() {
        return "List installed mods with ids, names, versions, and authors";
    }

    @Override
    public JsonObject inputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonObject query = new JsonObject();
        query.addProperty("type", "string");
        properties.add("query", query);
        JsonObject limit = new JsonObject();
        limit.addProperty("type", "integer");
        limit.addProperty("minimum", 1);
        limit.addProperty("maximum", 500);
        properties.add("limit", limit);
        schema.add("properties", properties);
        return schema;
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        String query = ToolArguments.string(invocation.arguments(), "query", "");
        int limit = ToolArguments.integer(invocation.arguments(), "limit", 200);
        return CompletableFuture.completedFuture(
                new ToolResult(invocation.callId(), ModListReport.format(mods(), query, limit), false, List.of()));
    }

    private synchronized List<ModEntry> mods() {
        long now = System.currentTimeMillis();
        if (!cache.isEmpty() && now - cachedAt < CACHE_MILLIS) {
            return cache;
        }
        List<ModEntry> loaded = new ArrayList<>();
        for (IModInfo info : ModList.get().getMods()) {
            if (SYSTEM_MODS.contains(info.getModId())) {
                continue;
            }
            loaded.add(new ModEntry(
                    info.getModId(), info.getDisplayName(), info.getVersion().toString(), authors(info)));
        }
        loaded.sort(Comparator.comparing(ModEntry::name, String.CASE_INSENSITIVE_ORDER));
        cache = List.copyOf(loaded);
        cachedAt = now;
        return cache;
    }

    private static String authors(IModInfo info) {
        try {
            Object raw = info.getConfig().<Object>getConfigElement("authors").orElse("");
            if (raw instanceof List<?> list) {
                List<String> names = new ArrayList<>();
                for (Object item : list) {
                    if (item != null) {
                        names.add(item.toString());
                    }
                }
                return String.join(", ", names);
            }
            return raw.toString();
        } catch (RuntimeException ignored) {
            return "";
        }
    }
}
