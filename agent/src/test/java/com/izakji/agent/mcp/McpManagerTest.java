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
                Map.of("server", client), Map.of("server", McpTestSupport.config("server")));
        manager.start("server");
        AgentTool tool = manager.tools().find("mcp__server__inspect").orElseThrow();
        assertEquals(ToolRisk.READ_ONLY, tool.risk());
        assertEquals("mcp__server__inspect", tool.name());
    }

    @Test
    void missingAnnotationsRequireApproval() {
        McpToolDescriptor descriptor = McpTestSupport.descriptor("mutate", false, false);
        FakeMcpClient client = new FakeMcpClient(List.of(descriptor), List.of(), List.of());
        McpManager manager = new McpManager(
                Map.of("server", client), Map.of("server", McpTestSupport.config("server")));
        manager.start("server");
        assertEquals(
                ToolRisk.MUTATING,
                manager.tools().find("mcp__server__mutate").orElseThrow().risk());
    }
}
