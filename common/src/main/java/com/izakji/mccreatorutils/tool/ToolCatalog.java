package com.izakji.mccreatorutils.tool;

import java.util.List;

/**
 * Ordered catalog of tools available in the Creator Utils interface.
 *
 * @author Izakji
 */
public final class ToolCatalog {
    public static final ToolDescriptor CAMERA =
            new ToolDescriptor(ToolId.CAMERA, "tool.mccreatorutils.camera", "cmdcam");

    public static final ToolDescriptor WORLD_TIME =
            new ToolDescriptor(ToolId.WORLD_TIME, "tool.mccreatorutils.world_time", "");

    public static final ToolDescriptor BIOMES = new ToolDescriptor(ToolId.BIOMES, "tool.mccreatorutils.biomes", "");

    public static final ToolDescriptor STRUCTURES =
            new ToolDescriptor(ToolId.STRUCTURES, "tool.mccreatorutils.structures", "");

    public static final ToolDescriptor DIMENSIONS =
            new ToolDescriptor(ToolId.DIMENSIONS, "tool.mccreatorutils.dimensions", "");

    public static final ToolDescriptor ITEMS = new ToolDescriptor(ToolId.ITEMS, "tool.mccreatorutils.items", "");

    public static final ToolDescriptor MOBS = new ToolDescriptor(ToolId.MOBS, "tool.mccreatorutils.mobs", "");

    public static final ToolDescriptor PLAYER = new ToolDescriptor(ToolId.PLAYER, "tool.mccreatorutils.player", "");

    public static final ToolDescriptor EXPLORE_MODS =
            new ToolDescriptor(ToolId.EXPLORE_MODS, "tool.mccreatorutils.explore_mods", "rinku");

    public static final ToolDescriptor AGENT = new ToolDescriptor(ToolId.AGENT, "tool.mccreatorutils.agent", "");

    private static final List<ToolDescriptor> TOOLS =
            List.of(CAMERA, WORLD_TIME, BIOMES, STRUCTURES, DIMENSIONS, ITEMS, MOBS, PLAYER, EXPLORE_MODS, AGENT);

    private ToolCatalog() {}

    public static List<ToolDescriptor> tools() {
        return TOOLS;
    }
}
