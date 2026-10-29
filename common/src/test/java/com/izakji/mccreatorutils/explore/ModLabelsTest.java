package com.izakji.mccreatorutils.explore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ModLabelsTest {

    @Test
    void labelsInstalledMod() {
        var mod = new InstalledMod("example", "Example Mod", "1.0", List.of());
        assertEquals("Example Mod (example)", ModLabels.label(mod));
    }

    @Test
    void nullModHasEmptyLabel() {
        assertEquals("", ModLabels.label(null));
    }
}
