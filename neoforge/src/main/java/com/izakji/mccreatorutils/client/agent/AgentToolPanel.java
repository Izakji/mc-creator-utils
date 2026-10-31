package com.izakji.mccreatorutils.client.agent;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;

/**
 * Agent tab host for conversations and provider configuration.
 *
 * @author Izakji
 */
public final class AgentToolPanel extends UIElement {
    private final Button chatTab = tab("tool.mccreatorutils.agent.tab.chat");
    private final Button providerTab = tab("tool.mccreatorutils.agent.tab.provider");
    private final UIElement content = new UIElement();
    private final AgentChatTab chat = new AgentChatTab();
    private final UIElement providerPlaceholder = new UIElement();
    private boolean chatSelected = true;

    public AgentToolPanel() {
        this.layout(layout -> layout.widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(4));

        var tabs = new UIElement();
        tabs.layout(layout -> layout.widthPercent(100)
                .height(16)
                .flexDirection(FlexDirection.ROW)
                .gapAll(4));
        chatTab.setOnClick(event -> select(true));
        providerTab.setOnClick(event -> select(false));
        tabs.addChildren(chatTab, providerTab);
        addChild(tabs);

        content.layout(layout -> layout.widthPercent(100).flex(1));
        providerPlaceholder.layout(layout -> layout.widthPercent(100).heightPercent(100));
        var placeholder = new Label();
        placeholder.setText(Component.translatable("tool.mccreatorutils.agent.provider_placeholder"));
        providerPlaceholder.addChild(placeholder);
        addChild(content);
        select(true);
    }

    private static Button tab(String translationKey) {
        var button = new Button();
        button.setText(Component.translatable(translationKey));
        button.layout(layout -> layout.width(80).height(16));
        return button;
    }

    private void select(boolean chatSelected) {
        this.chatSelected = chatSelected;
        chatTab.buttonStyle(style -> style.baseTexture(chatSelected ? Sprites.RECT_RD_LIGHT : Sprites.RECT_RD));
        providerTab.buttonStyle(style -> style.baseTexture(chatSelected ? Sprites.RECT_RD : Sprites.RECT_RD_LIGHT));
        content.clearAllChildren();
        content.addChild(chatSelected ? chat : providerPlaceholder);
    }
}
