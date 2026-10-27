package com.izakji.mccreatorutils.client;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import net.minecraft.network.chat.Component;

/**
 * Status label that displays the result of a client command dispatch.
 *
 * @author Izakji
 */
public final class CommandStatusLabel extends Label {
    public CommandStatusLabel() {
        setText(Component.empty());
        this.layout(layout -> layout.widthPercent(100));
    }

    public void runCommand(String command) {
        ClientCommandRunner.CommandResult result = ClientCommandRunner.execute(command);
        setText(result.message());
        textStyle(style -> style.textColor(result.success() ? ColorPattern.GREEN.color : ColorPattern.RED.color));
    }
}
