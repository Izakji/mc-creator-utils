package com.izakji.agent.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolRisk;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class McpManagerTest {

    @Test
    void namespacesToolsAndMapsAnnotationsToRisk() {
        McpToolDescriptor descriptor = McpTestSupport.descriptor("inspect", true, false);
        FakeMcpClient client = new FakeMcpClient(List.of(descriptor), List.of(), List.of());
        McpManager manager = new McpManager(
                Map.of("server", () -> client), Map.of("server", McpTestSupport.config("server")));
        manager.start("server");
        AgentTool tool = manager.tools().find("mcp__server__inspect").orElseThrow();
        assertEquals(ToolRisk.READ_ONLY, tool.risk());
        assertEquals("mcp__server__inspect", tool.name());
        org.junit.jupiter.api.Assertions.assertTrue(
                manager.tools().find("mcp__server__read_resource").isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(
                manager.tools().find("mcp__server__load_prompt").isEmpty());
    }

    @Test
    void missingAnnotationsRequireApproval() {
        McpToolDescriptor descriptor = McpTestSupport.descriptor("mutate", false, false);
        FakeMcpClient client = new FakeMcpClient(List.of(descriptor), List.of(), List.of());
        McpManager manager = new McpManager(
                Map.of("server", () -> client), Map.of("server", McpTestSupport.config("server")));
        manager.start("server");
        assertEquals(
                ToolRisk.MUTATING,
                manager.tools().find("mcp__server__mutate").orElseThrow().risk());
    }

    @Test
    void startingAServerDoesNotCloseItsClient() {
        FakeMcpClient client =
                new FakeMcpClient(List.of(McpTestSupport.descriptor("inspect", true, false)), List.of(), List.of());
        McpManager manager = new McpManager(
                Map.of("server", () -> client), Map.of("server", McpTestSupport.config("server")));
        manager.start("server");
        org.junit.jupiter.api.Assertions.assertFalse(client.closed());
    }

    @Test
    void restartCreatesAFreshClient() {
        java.util.concurrent.atomic.AtomicInteger created = new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.Supplier<McpClient> factory = () -> {
            created.incrementAndGet();
            return new FakeMcpClient(
                    List.of(McpTestSupport.descriptor("inspect", true, false)), List.of(), List.of());
        };
        McpManager manager = new McpManager(
                Map.of("server", factory), Map.of("server", McpTestSupport.config("server")));
        manager.start("server");
        manager.stop("server");
        manager.start("server");
        assertEquals(2, created.get());
    }
}
