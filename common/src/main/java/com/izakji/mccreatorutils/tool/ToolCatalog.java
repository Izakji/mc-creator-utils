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

    public static final ToolDescriptor ITEMS = new ToolDescriptor(ToolId.ITEMS, "tool.mccreatorutils.items", "");

    public static final ToolDescriptor MOBS = new ToolDescriptor(ToolId.MOBS, "tool.mccreatorutils.mobs", "");

    private static final List<ToolDescriptor> TOOLS = List.of(CAMERA, WORLD_TIME, BIOMES, STRUCTURES, ITEMS, MOBS);

    private ToolCatalog() {}

    public static List<ToolDescriptor> tools() {
        return TOOLS;
    }
}
