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

    private static final List<ToolDescriptor> TOOLS = List.of(CAMERA);

    private ToolCatalog() {}

    public static List<ToolDescriptor> tools() {
        return TOOLS;
    }
}
