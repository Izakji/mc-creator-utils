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

    private static final List<ToolDescriptor> TOOLS = List.of(CAMERA, WORLD_TIME);

    private ToolCatalog() {}

    public static List<ToolDescriptor> tools() {
        return TOOLS;
    }
}
