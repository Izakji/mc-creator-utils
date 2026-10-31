package com.izakji.mccreatorutils.client.agent.context;

/**
 * One installed mod row.
 *
 * @param id mod id
 * @param name display name
 * @param version version
 * @param authors authors, or an empty string
 * @author Izakji
 */
public record ModEntry(String id, String name, String version, String authors) {}
