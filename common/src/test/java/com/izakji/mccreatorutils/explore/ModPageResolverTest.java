package com.izakji.mccreatorutils.explore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ModPageResolverTest {

    @Test
    void prefersCurseForgeMetadata() {
        var mod = new InstalledMod(
                "example",
                "Example",
                "1.0",
                List.of("https://modrinth.com/mod/example", "https://www.curseforge.com/minecraft/mc-mods/example"));
        assertEquals(
                ModPlatform.CURSEFORGE,
                ModPageResolver.fromMetadata(mod).orElseThrow().platform());
    }

    @Test
    void fallsBackToModrinthMetadata() {
        var mod = new InstalledMod(
                "example",
                "Example",
                "1.0",
                List.of("https://github.com/owner/example", "https://modrinth.com/mod/example"));
        assertEquals(
                ModPlatform.MODRINTH,
                ModPageResolver.fromMetadata(mod).orElseThrow().platform());
    }

    @Test
    void returnsEmptyWhenNoPlatformMetadataExists() {
        var mod = new InstalledMod("example", "Example", "1.0", List.of("https://github.com/owner/example"));
        assertTrue(ModPageResolver.fromMetadata(mod).isEmpty());
    }
}
