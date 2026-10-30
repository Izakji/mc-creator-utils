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

    /**
     * Reads one argument from a translated chat message.
     *
     * <p>Locate success messages put the located biome or structure name in the first argument. The
     * client uses it to reject a late result that belongs to an earlier request.
     *
     * @param message chat message
     * @param key translation key to find
     * @param index zero-based argument index
     * @return argument text, or empty when the key or argument is absent
     */
    public static Optional<String> translationArgumentText(Component message, String key, int index) {
        if (message == null || key == null || index < 0) {
            return Optional.empty();
        }
        if (message.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals(key)) {
            Object[] arguments = contents.getArgs();
            if (index < arguments.length) {
                return argumentText(arguments[index]);
            }
        }
        for (Component sibling : message.getSiblings()) {
            Optional<String> found = translationArgumentText(sibling, key, index);
            if (found.isPresent()) {
                return found;
            }
        }
        if (message.getContents() instanceof TranslatableContents contents) {
            for (Object argument : contents.getArgs()) {
                if (argument instanceof Component component) {
                    Optional<String> found = translationArgumentText(component, key, index);
                    if (found.isPresent()) {
                        return found;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> argumentText(Object argument) {
        if (argument instanceof Component component) {
            return Optional.of(component.getString());
        }
        if (argument instanceof String text) {
            return Optional.of(text);
        }
        return Optional.empty();
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
