package com.izakji.agent.mcp;

import java.io.IOException;

/**
 * Creates an MCP stdio process.
 *
 * @author Izakji
 */
@FunctionalInterface
public interface ProcessFactory {
    Process start(ProcessBuilder builder) throws IOException;
}
