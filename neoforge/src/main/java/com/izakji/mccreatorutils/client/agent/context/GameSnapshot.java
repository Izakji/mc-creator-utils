package com.izakji.mccreatorutils.client.agent.context;

/**
 * Bounded, display-safe snapshot of the current client game state.
 *
 * @author Izakji
 */
public record GameSnapshot(
        String playerName,
        String gameMode,
        float health,
        float maxHealth,
        int food,
        int experienceLevel,
        int armor,
        String heldItem,
        String dimension,
        String biome,
        int x,
        int y,
        int z,
        long dayTime,
        String weather,
        String target,
        String screen,
        int fps,
        int ping,
        long memoryUsedMb,
        long memoryMaxMb,
        int renderDistance,
        int modCount) {

    /**
     * Formats the snapshot as compact prompt text.
     *
     * @return snapshot text
     */
    public String format() {
        return "Player: " + playerName + " (" + gameMode + "), health " + health + "/" + maxHealth
                + ", food " + food + ", xp " + experienceLevel + ", armor " + armor + ", held " + heldItem + "\n"
                + "World: " + dimension + ", biome " + biome + ", position " + x + " " + y + " " + z
                + ", day " + dayTime + ", " + weather + "\n"
                + "Target: " + target + "\n"
                + "Screen: " + screen + "\n"
                + "Performance: " + fps + " fps, " + ping + " ms ping, " + memoryUsedMb + "/"
                + memoryMaxMb + " MiB, render distance " + renderDistance + ", " + modCount + " mods";
    }
}
