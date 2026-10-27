package com.izakji.mccreatorutils.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.izakji.mccreatorutils.camera.CamMode;
import com.izakji.mccreatorutils.camera.DurationUnit;
import com.izakji.mccreatorutils.time.TimePreset;
import com.izakji.mccreatorutils.weather.WeatherUnit;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class CreatorToolSessionTest {

    @Test
    void defaultsMatchFirstOpenState() {
        var session = new CreatorToolSession();
        assertNull(session.cameraMode());
        assertEquals("", session.cameraDurationRaw());
        assertEquals(DurationUnit.SECONDS, session.cameraDurationUnit());
        assertEquals(TimePreset.DAY, session.timePreset());
        assertEquals("", session.timeTicksRaw());
        assertEquals(WeatherUnit.SECONDS, session.thunderUnit());
        assertEquals("", session.thunderDurationRaw());
        assertFalse(session.freezeTime());
    }

    @Test
    void storesCameraSelections() {
        var session = new CreatorToolSession();
        session.setCameraMode(CamMode.OUTSIDE);
        session.setCameraDurationRaw("20");
        session.setCameraDurationUnit(DurationUnit.MINUTES);

        assertEquals(CamMode.OUTSIDE, session.cameraMode());
        assertEquals("20", session.cameraDurationRaw());
        assertEquals(DurationUnit.MINUTES, session.cameraDurationUnit());
    }

    @Test
    void storesTimeAndWeatherSelections() {
        var session = new CreatorToolSession();
        session.setTimePreset(TimePreset.NIGHT);
        session.setTimeTicksRaw("6000");
        session.setThunderDurationRaw("2");
        session.setThunderUnit(WeatherUnit.HOURS);
        session.setFreezeTime(true);

        assertEquals(TimePreset.NIGHT, session.timePreset());
        assertEquals("6000", session.timeTicksRaw());
        assertEquals("2", session.thunderDurationRaw());
        assertEquals(WeatherUnit.HOURS, session.thunderUnit());
        assertTrue(session.freezeTime());
    }

    @Test
    void nullRawValuesNormalizeToEmpty() {
        var session = new CreatorToolSession();
        session.setCameraDurationRaw(null);
        session.setTimeTicksRaw(null);
        session.setThunderDurationRaw(null);

        assertEquals("", session.cameraDurationRaw());
        assertEquals("", session.timeTicksRaw());
        assertEquals("", session.thunderDurationRaw());
    }

    @Test
    void nonNullSelectorsRejectNull() {
        var session = new CreatorToolSession();
        assertThrows(NullPointerException.class, () -> session.setCameraDurationUnit(null));
        assertThrows(NullPointerException.class, () -> session.setTimePreset(null));
        assertThrows(NullPointerException.class, () -> session.setThunderUnit(null));
    }

    @Test
    void singletonIsStable() {
        assertSame(CreatorToolSession.instance(), CreatorToolSession.instance());
    }

    @Test
    void registryDefaultsAreEmpty() {
        var session = new CreatorToolSession();
        assertNull(session.biomeSelection());
        assertNull(session.structureSelection());
        assertNull(session.itemSelection());
        assertNull(session.mobSelection());
        assertEquals("1", session.itemCountRaw());
    }

    @Test
    void storesRegistrySelections() {
        var session = new CreatorToolSession();
        var id = ResourceLocation.parse("minecraft:plains");
        session.setBiomeSelection(id);
        session.setStructureSelection(id);
        session.setItemSelection(id);
        session.setMobSelection(id);
        session.setItemCountRaw("64");
        assertEquals(id, session.biomeSelection());
        assertEquals(id, session.structureSelection());
        assertEquals(id, session.itemSelection());
        assertEquals(id, session.mobSelection());
        assertEquals("64", session.itemCountRaw());
    }

    @Test
    void nullRegistryValuesNormalize() {
        var session = new CreatorToolSession();
        session.setBiomeSelection(ResourceLocation.parse("minecraft:plains"));
        session.setBiomeSelection(null);
        session.setItemCountRaw(null);
        assertNull(session.biomeSelection());
        assertEquals("1", session.itemCountRaw());
    }
}
