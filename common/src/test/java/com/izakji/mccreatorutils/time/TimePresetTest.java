package com.izakji.mccreatorutils.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TimePresetTest {

    @Test
    void presetsUseVanillaCommandIdentifiers() {
        assertEquals("day", TimePreset.DAY.id());
        assertEquals("night", TimePreset.NIGHT.id());
        assertEquals("noon", TimePreset.NOON.id());
        assertEquals("midnight", TimePreset.MIDNIGHT.id());
    }

    @Test
    void presetsExposeTranslationKeys() {
        assertEquals("tool.mccreatorutils.world_time.preset.day", TimePreset.DAY.translationKey());
        assertEquals("tool.mccreatorutils.world_time.preset.midnight", TimePreset.MIDNIGHT.translationKey());
    }
}
