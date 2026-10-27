package com.izakji.mccreatorutils.locate;

import java.util.Optional;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Reads vanilla locate chat feedback without depending on localized text.
 *
 * @author Izakji
 */
public final class LocateFeedback {
    private static final String TELEPORT_PREFIX = "/tp @s ";

    private LocateFeedback() {}

    public static boolean containsTranslation(Component message, String key) {
        if (message == null || key == null) {
            return false;
        }
        if (message.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals(key)) {
            return true;
        }
        for (Component sibling : message.getSiblings()) {
            if (containsTranslation(sibling, key)) {
                return true;
            }
        }
        if (message.getContents() instanceof TranslatableContents contents) {
            for (Object argument : contents.getArgs()) {
                if (argument instanceof Component component && containsTranslation(component, key)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Optional<String> findTeleportCommand(Component message) {
        if (message == null) {
            return Optional.empty();
        }
        ClickEvent clickEvent = message.getStyle().getClickEvent();
        if (clickEvent != null
                && isTeleportAction(clickEvent.getAction())
                && clickEvent.getValue().trim().startsWith(TELEPORT_PREFIX)) {
            return Optional.of(clickEvent.getValue());
        }
        for (Component sibling : message.getSiblings()) {
            Optional<String> found = findTeleportCommand(sibling);
            if (found.isPresent()) {
                return found;
            }
        }
        if (message.getContents() instanceof TranslatableContents contents) {
            for (Object argument : contents.getArgs()) {
                if (argument instanceof Component component) {
                    Optional<String> found = findTeleportCommand(component);
                    if (found.isPresent()) {
                        return found;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isTeleportAction(ClickEvent.Action action) {
        return action == ClickEvent.Action.SUGGEST_COMMAND || action == ClickEvent.Action.RUN_COMMAND;
    }
}
