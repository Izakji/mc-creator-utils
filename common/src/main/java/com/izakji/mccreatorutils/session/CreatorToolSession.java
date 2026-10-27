package com.izakji.mccreatorutils.session;

import com.izakji.mccreatorutils.camera.CamMode;
import com.izakji.mccreatorutils.camera.DurationUnit;
import com.izakji.mccreatorutils.time.TimePreset;
import com.izakji.mccreatorutils.weather.WeatherUnit;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Holds the creator-tool control values for the current client session.
 *
 * <p>Values survive closing and reopening the GUI. The singleton is intentionally in-memory only;
 * restarting the game restores the defaults.
 *
 * @author Izakji
 */
public final class CreatorToolSession {
    private static final CreatorToolSession INSTANCE = new CreatorToolSession();

    private CamMode cameraMode;
    private String cameraDurationRaw = "";
    private DurationUnit cameraDurationUnit = DurationUnit.SECONDS;
    private TimePreset timePreset = TimePreset.DAY;
    private String timeTicksRaw = "";
    private WeatherUnit thunderUnit = WeatherUnit.SECONDS;
    private String thunderDurationRaw = "";
    private boolean freezeTime;
    private ResourceLocation biomeSelection;
    private ResourceLocation structureSelection;
    private ResourceLocation dimensionSelection;
    private ResourceLocation itemSelection;
    private String itemCountRaw = "1";
    private ResourceLocation mobSelection;

    public CreatorToolSession() {}

    public static CreatorToolSession instance() {
        return INSTANCE;
    }

    public CamMode cameraMode() {
        return cameraMode;
    }

    public void setCameraMode(CamMode cameraMode) {
        this.cameraMode = cameraMode;
    }

    public String cameraDurationRaw() {
        return cameraDurationRaw;
    }

    public void setCameraDurationRaw(String cameraDurationRaw) {
        this.cameraDurationRaw = cameraDurationRaw == null ? "" : cameraDurationRaw;
    }

    public DurationUnit cameraDurationUnit() {
        return cameraDurationUnit;
    }

    public void setCameraDurationUnit(DurationUnit cameraDurationUnit) {
        this.cameraDurationUnit = Objects.requireNonNull(cameraDurationUnit, "cameraDurationUnit");
    }

    public TimePreset timePreset() {
        return timePreset;
    }

    public void setTimePreset(TimePreset timePreset) {
        this.timePreset = Objects.requireNonNull(timePreset, "timePreset");
    }

    public String timeTicksRaw() {
        return timeTicksRaw;
    }

    public void setTimeTicksRaw(String timeTicksRaw) {
        this.timeTicksRaw = timeTicksRaw == null ? "" : timeTicksRaw;
    }

    public WeatherUnit thunderUnit() {
        return thunderUnit;
    }

    public void setThunderUnit(WeatherUnit thunderUnit) {
        this.thunderUnit = Objects.requireNonNull(thunderUnit, "thunderUnit");
    }

    public String thunderDurationRaw() {
        return thunderDurationRaw;
    }

    public void setThunderDurationRaw(String thunderDurationRaw) {
        this.thunderDurationRaw = thunderDurationRaw == null ? "" : thunderDurationRaw;
    }

    public boolean freezeTime() {
        return freezeTime;
    }

    public void setFreezeTime(boolean freezeTime) {
        this.freezeTime = freezeTime;
    }

    public ResourceLocation biomeSelection() {
        return biomeSelection;
    }

    public void setBiomeSelection(ResourceLocation biomeSelection) {
        this.biomeSelection = biomeSelection;
    }

    public ResourceLocation structureSelection() {
        return structureSelection;
    }

    public void setStructureSelection(ResourceLocation structureSelection) {
        this.structureSelection = structureSelection;
    }

    public ResourceLocation dimensionSelection() {
        return dimensionSelection;
    }

    public void setDimensionSelection(ResourceLocation dimensionSelection) {
        this.dimensionSelection = dimensionSelection;
    }

    public ResourceLocation itemSelection() {
        return itemSelection;
    }

    public void setItemSelection(ResourceLocation itemSelection) {
        this.itemSelection = itemSelection;
    }

    public String itemCountRaw() {
        return itemCountRaw;
    }

    public void setItemCountRaw(String itemCountRaw) {
        this.itemCountRaw = itemCountRaw == null ? "1" : itemCountRaw;
    }

    public ResourceLocation mobSelection() {
        return mobSelection;
    }

    public void setMobSelection(ResourceLocation mobSelection) {
        this.mobSelection = mobSelection;
    }
}
