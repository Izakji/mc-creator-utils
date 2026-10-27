package com.izakji.mccreatorutils.locate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.junit.jupiter.api.Test;

class LocateFeedbackTest {

    private static Component clickable(ClickEvent.Action action, String command) {
        return Component.literal("[coords]").withStyle(style -> style.withClickEvent(new ClickEvent(action, command)));
    }

    @Test
    void findsTeleportInTranslatableArguments() {
        var message = Component.translatable(
                "commands.locate.biome.success",
                "minecraft:plains",
                clickable(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s 10 64 -20"),
                42);

        assertTrue(LocateFeedback.containsTranslation(message, "commands.locate.biome.success"));
        assertEquals(Optional.of("/tp @s 10 64 -20"), LocateFeedback.findTeleportCommand(message));
    }

    @Test
    void findsTeleportInSibling() {
        MutableComponent message = Component.empty();
        message.append(Component.literal("Found it "));
        message.append(clickable(ClickEvent.Action.RUN_COMMAND, "/tp @s 1 2 3"));

        assertEquals(Optional.of("/tp @s 1 2 3"), LocateFeedback.findTeleportCommand(message));
    }

    @Test
    void findsTeleportInNestedTranslatedArgument() {
        var nested = Component.translatable(
                "chat.coordinates", clickable(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s 4 5 6"));
        var message = Component.translatable("commands.locate.structure.success", "id", nested, 1);

        assertEquals(Optional.of("/tp @s 4 5 6"), LocateFeedback.findTeleportCommand(message));
    }

    @Test
    void ignoresUnrelatedMessagesAndCommands() {
        var unrelated = Component.literal("hello");
        assertFalse(LocateFeedback.containsTranslation(unrelated, "commands.locate.biome.success"));
        assertTrue(LocateFeedback.findTeleportCommand(unrelated).isEmpty());

        var wrongCommand = Component.translatable(
                "commands.locate.biome.success",
                "minecraft:plains",
                clickable(ClickEvent.Action.SUGGEST_COMMAND, "/give @s minecraft:stone"),
                1);
        assertTrue(LocateFeedback.findTeleportCommand(wrongCommand).isEmpty());
    }
}
