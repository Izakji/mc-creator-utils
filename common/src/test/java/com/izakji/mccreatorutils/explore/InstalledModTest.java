package com.izakji.mccreatorutils.explore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class InstalledModTest {

    @Test
    void normalizesNullsAndSearchText() {
        var mod = new InstalledMod(null, null, null, null);
        assertEquals("", mod.modId());
        assertEquals("", mod.displayName());
        assertEquals("", mod.version());
        assertTrue(mod.metadataUrls().isEmpty());

        var named = new InstalledMod("example", "Example Mod", "1.0", List.of("https://modrinth.com/mod/example"));
        assertEquals("Example Mod example", named.searchText());
        assertThrows(
                UnsupportedOperationException.class, () -> named.metadataUrls().add("https://example.invalid"));
    }
}
