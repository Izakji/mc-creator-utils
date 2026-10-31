package com.izakji.mccreatorutils.client;

import com.izakji.mccreatorutils.McCreatorUtils;
import com.izakji.mccreatorutils.client.agent.AgentChatSession;
import com.izakji.mccreatorutils.client.agent.AgentChatWindow;
import com.izakji.mccreatorutils.client.browser.BrowserWorkspace;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only entrypoint that registers the Creator Utils keybind.
 *
 * @author Izakji
 */
@Mod(value = McCreatorUtils.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = McCreatorUtils.MOD_ID, value = Dist.CLIENT)
public final class McCreatorUtilsClient {
    public static final KeyMapping OPEN_GUI = new KeyMapping(
            "key.mccreatorutils.open_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_PERIOD,
            "key.categories.mccreatorutils");
    public static final KeyMapping FOCUS_BROWSER = new KeyMapping(
            "key.mccreatorutils.focus_browser",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.mccreatorutils");
    public static final KeyMapping TOGGLE_BROWSER = new KeyMapping(
            "key.mccreatorutils.toggle_browser",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_BRACKET,
            "key.categories.mccreatorutils");
    public static final KeyMapping OPEN_AGENT_CHAT = new KeyMapping(
            "key.mccreatorutils.agent_chat",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.mccreatorutils");

    @SubscribeEvent
    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GUI);
        event.register(FOCUS_BROWSER);
        event.register(TOGGLE_BROWSER);
        event.register(OPEN_AGENT_CHAT);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_GUI.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new CreatorToolsScreen());
            }
        }
        while (TOGGLE_BROWSER.consumeClick()) {
            if (minecraft.screen == null) {
                BrowserWorkspace.instance().toggleBrowser();
            }
        }
        while (FOCUS_BROWSER.consumeClick()) {
            if (minecraft.screen == null && BrowserWorkspace.instance().hasWindow()) {
                BrowserWorkspace.instance().focus();
            }
        }
        while (OPEN_AGENT_CHAT.consumeClick()) {
            if (minecraft.screen == null) {
                AgentChatWindow.instance().focusOrOpen();
            }
        }
    }

    @SubscribeEvent
    static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        BrowserWorkspace.instance().onLogout();
        if (AgentChatWindow.instance().visible()) {
            AgentChatSession.instance().onLogout();
            AgentChatWindow.instance().hide();
        }
    }
}
