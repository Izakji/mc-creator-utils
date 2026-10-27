package com.izakji.mccreatorutils.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class RegistryGroupingTest {

    @Test
    void groupsAndSortsEntries() {
        var groups = RegistryGrouping.group(List.of(
                ResourceLocation.parse("mod_b:beta"),
                ResourceLocation.parse("minecraft:plains"),
                ResourceLocation.parse("mod_a:zulu"),
                ResourceLocation.parse("minecraft:forest"),
                ResourceLocation.parse("mod_a:alpha")));

        assertEquals(
                List.of("minecraft", "mod_a", "mod_b"),
                groups.stream().map(RegistryGroup::namespace).toList());
        assertEquals(
                List.of(ResourceLocation.parse("minecraft:forest"), ResourceLocation.parse("minecraft:plains")),
                groups.get(0).entries());
        assertEquals(
                List.of(ResourceLocation.parse("mod_a:alpha"), ResourceLocation.parse("mod_a:zulu")),
                groups.get(1).entries());
    }

    @Test
    void emptyInputYieldsNoGroups() {
        assertTrue(RegistryGrouping.group(List.of()).isEmpty());
    }
}
