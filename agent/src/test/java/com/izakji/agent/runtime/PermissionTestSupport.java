package com.izakji.agent.runtime;

import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import com.izakji.agent.config.PermissionRules;
import com.izakji.agent.conversation.Conversation;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

final class PermissionTestSupport {
    private PermissionTestSupport() {}

    static PermissionGate gate(PermissionRules rules) {
        return new PermissionGate(rules, List.of());
    }

    static PermissionRules defaultRules() {
        return new PermissionRules(true, Set.of(), Set.of(), Set.of());
    }

    static AgentTool readOnlyTool() {
        return tool("read", ToolRisk.READ_ONLY);
    }

    static AgentTool mutatingTool() {
        return tool("mutate", ToolRisk.MUTATING);
    }

    static AgentTool dangerousTool() {
        return tool("danger", ToolRisk.DANGEROUS);
    }

    static ToolInvocation invocation(String name) {
        return new ToolInvocation("call-1", name, new JsonObject());
    }

    static Conversation conversation() {
        Instant now = Instant.now();
        return new Conversation("c", "Chat", now, now, "main", List.of(), Set.of());
    }

    private static AgentTool tool(String name, ToolRisk risk) {
        return new AgentTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return name;
            }

            @Override
            public JsonObject inputSchema() {
                return new JsonObject();
            }

            @Override
            public ToolRisk risk() {
                return risk;
            }

            @Override
            public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
                return CompletableFuture.completedFuture(new ToolResult(invocation.callId(), "", false, List.of()));
            }
        };
    }
}
