package com.izakji.mccreatorutils.client;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import net.minecraft.network.chat.Component;

/**
 * Screen that hosts the Creator Utils modular interface.
 *
 * @author Izakji
 */
public final class CreatorToolsScreen extends ModularUIScreen {
    public CreatorToolsScreen() {
        super(CreatorToolsUi.create(), Component.translatable("screen.mccreatorutils.creator_tools"));
    }
}
