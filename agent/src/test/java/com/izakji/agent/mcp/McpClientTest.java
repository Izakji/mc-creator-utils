package com.izakji.agent.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class McpClientTest {

    @Test
    void initializesAndListsTools() {
        FakeTransport transport = new FakeTransport();
        McpClient client = new DefaultMcpClient(transport);
        CompletableFuture<McpServerInfo> init = client.initialize();
        transport.receive("""
                {"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2025-06-18",
                "capabilities":{"tools":{}},"serverInfo":{"name":"fake","version":"1"}}}
                """);
        assertEquals("fake", init.join().name());

        CompletableFuture<List<McpToolDescriptor>> tools = client.listTools();
        transport.receive("""
                {"jsonrpc":"2.0","id":2,"result":{"tools":[{"name":"lookup",
                "description":"Lookup","inputSchema":{"type":"object"}}]}}
                """);
        assertEquals("lookup", tools.join().get(0).name());
    }
}
