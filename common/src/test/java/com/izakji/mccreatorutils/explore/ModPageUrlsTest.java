package com.izakji.mccreatorutils.explore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModPageUrlsTest {

    @Test
    void normalizesCurseForgeProjectPaths() {
        assertEquals(
                new ResolvedModPage(
                        ModPlatform.CURSEFORGE, URI.create("https://www.curseforge.com/minecraft/mc-mods/jei")),
                ModPageUrls.parse("https://www.curseforge.com/minecraft/mc-mods/jei/files?gameVersion=1.21.1")
                        .orElseThrow());
    }

    @Test
    void normalizesModrinthProjectPaths() {
        assertEquals(
                new ResolvedModPage(ModPlatform.MODRINTH, URI.create("https://modrinth.com/mod/async-locator-refined")),
                ModPageUrls.parse("https://modrinth.com/mod/async-locator-refined/versions")
                        .orElseThrow());
    }

    @Test
    void ignoresUnsupportedUrls() {
        assertTrue(ModPageUrls.parse("https://github.com/TheIllusiveC4/Curios").isEmpty());
        assertTrue(ModPageUrls.parse("not a url").isEmpty());
        assertTrue(ModPageUrls.parse(null).isEmpty());
    }

    @Test
    void createsDeterministicSlugCandidates() {
        var mod = new InstalledMod("a_man_with_plushies", "A Man With Plushies", "5.0.3", List.of());
        assertEquals(List.of("a-man-with-plushies"), ModPageUrls.slugCandidates(mod));
    }
}
