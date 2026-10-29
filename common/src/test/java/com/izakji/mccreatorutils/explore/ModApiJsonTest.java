package com.izakji.mccreatorutils.explore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModApiJsonTest {
    private static final InstalledMod MOD = new InstalledMod("example", "Example Mod", "1.0", List.of());

    @Test
    void curseForgeRequiresExactSlugOrNameMatch() {
        String matching = """
                {"data":[{"name":"Example Mod","slug":"example-mod",
                "links":{"websiteUrl":"https://www.curseforge.com/minecraft/mc-mods/example-mod"}}]}
                """;
        String fuzzy = """
                {"data":[{"name":"Example Mod Extra","slug":"example-mod-extra",
                "links":{"websiteUrl":"https://www.curseforge.com/minecraft/mc-mods/example-mod-extra"}}]}
                """;
        assertEquals(
                ModPlatform.CURSEFORGE,
                ModApiJson.curseForgeResult(matching, MOD).orElseThrow().platform());
        assertTrue(ModApiJson.curseForgeResult(fuzzy, MOD).isEmpty());
    }

    @Test
    void modrinthProjectUsesSlug() {
        String json = """
                {"title":"Example Mod","slug":"example-mod"}
                """;
        assertEquals(
                URI.create("https://modrinth.com/mod/example-mod"),
                ModApiJson.modrinthProjectResult(json).orElseThrow().uri());
    }

    @Test
    void modrinthSearchRequiresExactTitleOrSlug() {
        String exact = """
                {"hits":[{"title":"Example Mod","slug":"example-mod"}]}
                """;
        String fuzzy = """
                {"hits":[{"title":"Example Mod Extra","slug":"example-mod-extra"}]}
                """;
        assertEquals(
                ModPlatform.MODRINTH,
                ModApiJson.modrinthSearchResult(exact, MOD).orElseThrow().platform());
        assertTrue(ModApiJson.modrinthSearchResult(fuzzy, MOD).isEmpty());
    }

    @Test
    void malformedJsonReturnsEmpty() {
        assertTrue(ModApiJson.curseForgeResult("not json", MOD).isEmpty());
        assertTrue(ModApiJson.modrinthProjectResult("").isEmpty());
        assertTrue(ModApiJson.modrinthSearchResult("{}", MOD).isEmpty());
    }
}
