package com.izakji.agent.mcp;

/**
 * MCP resource content.
 *
 * @param uri resource URI
 * @param mimeType MIME type
 * @param text text content
 * @author Izakji
 */
public record McpResourceContent(String uri, String mimeType, String text) {}
