package com.izakji.mccreatorutils.client;

import com.izakji.mccreatorutils.tool.ToolCatalog;
import com.izakji.mccreatorutils.tool.ToolId;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;

/**
 * Screen that hosts the Creator Utils modular interface.
 *
 * @author Izakji
 */
public final class CreatorToolsScreen extends ModularUIScreen {
    public CreatorToolsScreen() {
        this(ToolCatalog.tools().get(0).id());
    }

    public CreatorToolsScreen(ToolId initialTool) {
        super(CreatorToolsUi.create(initialTool), Component.translatable("screen.mccreatorutils.creator_tools"));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (McCreatorUtilsClient.OPEN_GUI.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
