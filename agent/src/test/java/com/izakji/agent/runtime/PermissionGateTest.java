package com.izakji.agent.runtime;

import static com.izakji.agent.runtime.PermissionTestSupport.conversation;
import static com.izakji.agent.runtime.PermissionTestSupport.dangerousTool;
import static com.izakji.agent.runtime.PermissionTestSupport.defaultRules;
import static com.izakji.agent.runtime.PermissionTestSupport.gate;
import static com.izakji.agent.runtime.PermissionTestSupport.invocation;
import static com.izakji.agent.runtime.PermissionTestSupport.mutatingTool;
import static com.izakji.agent.runtime.PermissionTestSupport.readOnlyTool;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.izakji.agent.api.ToolRisk;
import com.izakji.agent.config.PermissionRules;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PermissionGateTest {

    @Test
    void readOnlyAutoApprovesWhenEnabled() {
        PermissionGate gate = gate(new PermissionRules(true, Set.of(), Set.of(), Set.of()));
        assertEquals(
                PermissionKind.ALLOW,
                gate.decide(readOnlyTool(), invocation("read"), conversation()).kind());
    }

    @Test
    void mutatingAsksAndConversationApprovalAllows() {
        PermissionGate gate = gate(new PermissionRules(true, Set.of(), Set.of(), Set.of()));
        assertEquals(
                PermissionKind.ASK,
                gate.decide(mutatingTool(), invocation("mutate"), conversation())
                        .kind());
        com.izakji.agent.conversation.Conversation approved = conversation()
                .withConversationApproval(
                        PermissionGate.approvalKey("mutate", new com.google.gson.JsonObject()), Instant.now());
        assertEquals(
                PermissionKind.ALLOW,
                gate.decide(mutatingTool(), invocation("mutate"), approved).kind());
    }

    @Test
    void conversationApprovalIsScopedToTheExactArguments() {
        PermissionGate gate = gate(new PermissionRules(true, Set.of(), Set.of(), Set.of()));
        com.google.gson.JsonObject first = new com.google.gson.JsonObject();
        first.addProperty("path", "a");
        com.google.gson.JsonObject second = new com.google.gson.JsonObject();
        second.addProperty("path", "b");
        com.izakji.agent.conversation.Conversation approved =
                conversation().withConversationApproval(PermissionGate.approvalKey("mutate", first), Instant.now());

        assertEquals(
                PermissionKind.ALLOW,
                gate.decide(
                                mutatingTool(),
                                new com.izakji.agent.api.ToolInvocation("call-1", "mutate", first),
                                approved)
                        .kind());
        assertEquals(
                PermissionKind.ASK,
                gate.decide(
                                mutatingTool(),
                                new com.izakji.agent.api.ToolInvocation("call-2", "mutate", second),
                                approved)
                        .kind());
    }

    @Test
    void dangerousRequiresExplicitEnable() {
        assertTrue(PermissionKind.DENY
                == gate(defaultRules())
                        .decide(dangerousTool(), invocation("danger"), conversation())
                        .kind());
        PermissionGate enabled = gate(new PermissionRules(true, Set.of(), Set.of(), Set.of("danger")));
        assertEquals(
                PermissionKind.ALLOW,
                enabled.decide(dangerousTool(), invocation("danger"), conversation())
                        .kind());
    }

    @Test
    void classifierCanRaiseRiskToDangerous() {
        ToolRiskClassifier classifier = (tool, call, declared) -> ToolRisk.DANGEROUS;
        PermissionGate gate =
                new PermissionGate(new PermissionRules(true, Set.of(), Set.of(), Set.of()), List.of(classifier));
        assertEquals(
                PermissionKind.DENY,
                gate.decide(readOnlyTool(), invocation("read"), conversation()).kind());
    }
}
