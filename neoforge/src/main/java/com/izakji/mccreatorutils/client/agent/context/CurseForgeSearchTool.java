package com.izakji.mccreatorutils.client.agent.context;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Read-only CurseForge mod search.
 *
 * @author Izakji
 */
public final class CurseForgeSearchTool implements AgentTool {
    private final CurseForgeClient client;

    public CurseForgeSearchTool(CurseForgeClient client) {
        this.client = client;
    }

    @Override
    public String name() {
        return "curseforge_search";
    }

    @Override
    public String description() {
        return "Search CurseForge Minecraft mods by text or slug. Pass gameVersion 1.21.1 for this instance. "
                + "Returns mod ids, slugs, authors, downloads, summaries, and latest file ids.";
    }

    @Override
    public JsonObject inputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        addString(properties, "query");
        addString(properties, "slug");
        addString(properties, "gameVersion");
        JsonObject modLoader = new JsonObject();
        modLoader.addProperty("type", "string");
        JsonArray loaders = new JsonArray();
        loaders.add("neoforge");
        loaders.add("forge");
        loaders.add("fabric");
        loaders.add("quilt");
        loaders.add("any");
        modLoader.add("enum", loaders);
        properties.add("modLoader", modLoader);
        JsonObject category = new JsonObject();
        category.addProperty("type", "integer");
        properties.add("categoryId", category);
        JsonObject sort = new JsonObject();
        sort.addProperty("type", "string");
        JsonArray sorts = new JsonArray();
        sorts.add("popularity");
        sorts.add("downloads");
        sorts.add("updated");
        sorts.add("name");
        sorts.add("featured");
        sort.add("enum", sorts);
        properties.add("sort", sort);
        JsonObject limit = new JsonObject();
        limit.addProperty("type", "integer");
        limit.addProperty("minimum", 1);
        limit.addProperty("maximum", 20);
        properties.add("limit", limit);
        JsonObject index = new JsonObject();
        index.addProperty("type", "integer");
        index.addProperty("minimum", 0);
        properties.add("index", index);
        schema.add("properties", properties);
        return schema;
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        JsonObject arguments = invocation.arguments();
        return client.search(
                        ToolArguments.string(arguments, "query", ""),
                        ToolArguments.string(arguments, "slug", ""),
                        ToolArguments.string(arguments, "gameVersion", ""),
                        Integer.parseInt(CurseForgeReport.normalizeModLoader(
                                ToolArguments.string(arguments, "modLoader", "neoforge"))),
                        ToolArguments.integer(arguments, "categoryId", 0),
                        Integer.parseInt(
                                CurseForgeReport.normalizeSort(ToolArguments.string(arguments, "sort", "popularity"))),
                        ToolArguments.integer(arguments, "limit", 10),
                        ToolArguments.integer(arguments, "index", 0))
                .thenApply(
                        text -> new ToolResult(invocation.callId(), text, CurseForgeClient.isError(text), List.of()));
    }

    private static void addString(JsonObject properties, String name) {
        JsonObject value = new JsonObject();
        value.addProperty("type", "string");
        properties.add(name, value);
    }
}
