package com.izakji.mccreatorutils.locate.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LocateSettingsTest {

    @Test
    void defaultsMatchDesign() {
        var settings = LocateSettings.defaults();

        assertTrue(settings.enabled());
        assertEquals(2, settings.biomeThreads());
        assertEquals(32, settings.maxQueuedSearches());
        assertEquals(60, settings.timeoutSeconds());
        assertTrue(settings.structurePrefilter());
        assertEquals(1, settings.structurePrefilterMarginChunks());
        assertEquals(30, settings.resultCacheSeconds());
        assertFalse(settings.debugStats());
    }
}
