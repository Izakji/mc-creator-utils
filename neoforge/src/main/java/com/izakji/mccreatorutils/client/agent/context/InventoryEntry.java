package com.izakji.mccreatorutils.client.agent.context;

/**
 * One non-empty inventory slot.
 *
 * @param slot slot index
 * @param itemId item id
 * @param count stack count
 * @param name display name
 * @param damage damage value
 * @author Izakji
 */
public record InventoryEntry(int slot, String itemId, int count, String name, int damage) {}
