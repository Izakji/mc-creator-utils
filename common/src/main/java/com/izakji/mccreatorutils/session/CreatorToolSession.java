package com.izakji.mccreatorutils.session;

import com.izakji.mccreatorutils.camera.CamMode;
import com.izakji.mccreatorutils.camera.DurationUnit;
import com.izakji.mccreatorutils.player.XpType;
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
    private String exploreModId;
    private boolean invincible;
    private boolean waterBreathing;
    private String xpAmountRaw = "";
    private XpType xpType = XpType.LEVELS;

    CreatorToolSession() {}

    /**
     * Returns the client-session singleton.
     *
     * @return shared session
     */
    public static CreatorToolSession instance() {
        return INSTANCE;
    }

    /**
     * Returns the selected camera mode.
     *
     * @return camera mode, or null before the first selection
     */
    public CamMode cameraMode() {
        return cameraMode;
    }

    /**
     * Selects a camera mode.
     *
     * @param cameraMode camera mode, or null to clear
     */
    public void setCameraMode(CamMode cameraMode) {
        this.cameraMode = cameraMode;
    }

    /**
     * Returns the raw camera duration text.
     *
     * @return duration text, never null
     */
    public String cameraDurationRaw() {
        return cameraDurationRaw;
    }

    /**
     * Stores the raw camera duration text.
     *
     * @param cameraDurationRaw duration text; null becomes an empty string
     */
    public void setCameraDurationRaw(String cameraDurationRaw) {
        this.cameraDurationRaw = cameraDurationRaw == null ? "" : cameraDurationRaw;
    }

    /**
     * Returns the camera duration unit.
     *
     * @return duration unit, never null
     */
    public DurationUnit cameraDurationUnit() {
        return cameraDurationUnit;
    }

    /**
     * Selects the camera duration unit.
     *
     * @param cameraDurationUnit duration unit
     */
    public void setCameraDurationUnit(DurationUnit cameraDurationUnit) {
        this.cameraDurationUnit = Objects.requireNonNull(cameraDurationUnit, "cameraDurationUnit");
    }

    /**
     * Returns the selected time preset.
     *
     * @return time preset, never null
     */
    public TimePreset timePreset() {
        return timePreset;
    }

    /**
     * Selects a time preset.
     *
     * @param timePreset time preset
     */
    public void setTimePreset(TimePreset timePreset) {
        this.timePreset = Objects.requireNonNull(timePreset, "timePreset");
    }

    /**
     * Returns the raw exact-time text.
     *
     * @return tick text, never null
     */
    public String timeTicksRaw() {
        return timeTicksRaw;
    }

    /**
     * Stores the raw exact-time text.
     *
     * @param timeTicksRaw tick text; null becomes an empty string
     */
    public void setTimeTicksRaw(String timeTicksRaw) {
        this.timeTicksRaw = timeTicksRaw == null ? "" : timeTicksRaw;
    }

    /**
     * Returns the thunder duration unit.
     *
     * @return weather unit, never null
     */
    public WeatherUnit thunderUnit() {
        return thunderUnit;
    }

    /**
     * Selects the thunder duration unit.
     *
     * @param thunderUnit weather unit
     */
    public void setThunderUnit(WeatherUnit thunderUnit) {
        this.thunderUnit = Objects.requireNonNull(thunderUnit, "thunderUnit");
    }

    /**
     * Returns the raw thunder duration text.
     *
     * @return duration text, never null
     */
    public String thunderDurationRaw() {
        return thunderDurationRaw;
    }

    /**
     * Stores the raw thunder duration text.
     *
     * @param thunderDurationRaw duration text; null becomes an empty string
     */
    public void setThunderDurationRaw(String thunderDurationRaw) {
        this.thunderDurationRaw = thunderDurationRaw == null ? "" : thunderDurationRaw;
    }

    /**
     * Returns whether time is frozen.
     *
     * @return true when the freeze-time toggle is enabled
     */
    public boolean freezeTime() {
        return freezeTime;
    }

    /**
     * Sets the freeze-time toggle.
     *
     * @param freezeTime true to freeze time
     */
    public void setFreezeTime(boolean freezeTime) {
        this.freezeTime = freezeTime;
    }

    /**
     * Returns the selected biome.
     *
     * @return biome id, or null when none is selected
     */
    public ResourceLocation biomeSelection() {
        return biomeSelection;
    }

    /**
     * Selects a biome.
     *
     * @param biomeSelection biome id, or null to clear
     */
    public void setBiomeSelection(ResourceLocation biomeSelection) {
        this.biomeSelection = biomeSelection;
    }

    /**
     * Returns the selected structure.
     *
     * @return structure id, or null when none is selected
     */
    public ResourceLocation structureSelection() {
        return structureSelection;
    }

    /**
     * Selects a structure.
     *
     * @param structureSelection structure id, or null to clear
     */
    public void setStructureSelection(ResourceLocation structureSelection) {
        this.structureSelection = structureSelection;
    }

    /**
     * Returns the selected dimension.
     *
     * @return dimension id, or null when none is selected
     */
    public ResourceLocation dimensionSelection() {
        return dimensionSelection;
    }

    /**
     * Selects a dimension.
     *
     * @param dimensionSelection dimension id, or null to clear
     */
    public void setDimensionSelection(ResourceLocation dimensionSelection) {
        this.dimensionSelection = dimensionSelection;
    }

    /**
     * Returns the selected item.
     *
     * @return item id, or null when none is selected
     */
    public ResourceLocation itemSelection() {
        return itemSelection;
    }

    /**
     * Selects an item.
     *
     * @param itemSelection item id, or null to clear
     */
    public void setItemSelection(ResourceLocation itemSelection) {
        this.itemSelection = itemSelection;
    }

    /**
     * Returns the raw item count text.
     *
     * @return item count text, never null
     */
    public String itemCountRaw() {
        return itemCountRaw;
    }

    /**
     * Stores the raw item count text.
     *
     * @param itemCountRaw count text; null becomes {@code "1"}
     */
    public void setItemCountRaw(String itemCountRaw) {
        this.itemCountRaw = itemCountRaw == null ? "1" : itemCountRaw;
    }

    /**
     * Returns the selected mob.
     *
     * @return mob id, or null when none is selected
     */
    public ResourceLocation mobSelection() {
        return mobSelection;
    }

    /**
     * Selects a mob.
     *
     * @param mobSelection mob id, or null to clear
     */
    public void setMobSelection(ResourceLocation mobSelection) {
        this.mobSelection = mobSelection;
    }

    /**
     * Returns the selected Explore Mods entry.
     *
     * @return mod id, or null when none is selected
     */
    public String exploreModId() {
        return exploreModId;
    }

    /**
     * Selects an Explore Mods entry.
     *
     * @param exploreModId mod id; null or blank clears the selection
     */
    public void setExploreModId(String exploreModId) {
        this.exploreModId = exploreModId == null || exploreModId.isBlank() ? null : exploreModId.strip();
    }

    /**
     * Returns whether invincibility is enabled.
     *
     * @return true when the effect toggle is enabled
     */
    public boolean invincible() {
        return invincible;
    }

    /**
     * Sets the invincibility toggle.
     *
     * @param invincible true to enable
     */
    public void setInvincible(boolean invincible) {
        this.invincible = invincible;
    }

    /**
     * Returns whether water breathing is enabled.
     *
     * @return true when the effect toggle is enabled
     */
    public boolean waterBreathing() {
        return waterBreathing;
    }

    /**
     * Sets the water-breathing toggle.
     *
     * @param waterBreathing true to enable
     */
    public void setWaterBreathing(boolean waterBreathing) {
        this.waterBreathing = waterBreathing;
    }

    /**
     * Returns the raw experience amount text.
     *
     * @return experience text, never null
     */
    public String xpAmountRaw() {
        return xpAmountRaw;
    }

    /**
     * Stores the raw experience amount text.
     *
     * @param xpAmountRaw experience text; null becomes an empty string
     */
    public void setXpAmountRaw(String xpAmountRaw) {
        this.xpAmountRaw = xpAmountRaw == null ? "" : xpAmountRaw;
    }

    /**
     * Returns the experience unit.
     *
     * @return experience type, never null
     */
    public XpType xpType() {
        return xpType;
    }

    /**
     * Selects the experience unit.
     *
     * @param xpType experience type
     */
    public void setXpType(XpType xpType) {
        this.xpType = Objects.requireNonNull(xpType, "xpType");
    }
}
