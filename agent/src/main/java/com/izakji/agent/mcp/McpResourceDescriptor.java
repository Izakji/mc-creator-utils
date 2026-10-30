package com.izakji.agent.mcp;

/**
 * MCP resource descriptor.
 *
 * @param uri resource URI
 * @param name display name
 * @param description description
 * @param mimeType MIME type
 * @author Izakji
 */
public record McpResourceDescriptor(String uri, String name, String description, String mimeType) {}
