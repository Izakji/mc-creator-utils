package com.izakji.agent.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.izakji.agent.api.ToolCall;
import com.izakji.agent.api.ToolRisk;
import com.izakji.agent.conversation.Conversation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AgentRuntimeToolTest {

    @Test
    void approvalRunsToolAndFeedsResultBack() throws Exception {
        FakeProvider provider = new FakeProvider(List.of(
                new FakeProvider.Turn(List.of(), List.of(), List.of(TestMessages.call("call-1"))),
                new FakeProvider.Turn(List.of("done"), List.of(), List.of())));
        FakeTool tool = new FakeTool("mutate", ToolRisk.MUTATING, "ok");
        try (AgentRuntime runtime =
                AgentRuntimeTestSupport.runtime(AgentRuntimeTestSupport.root(), provider, List.of(tool))) {
            Conversation conversation = runtime.createConversation("Chat", "main");
            RecordingObserver observer = new RecordingObserver();
            TurnHandle handle = runtime.startTurn(conversation.id(), "do it", observer);
            observer.awaitApproval();
            runtime.approve(conversation.id(), observer.approval().id(), ApprovalChoice.APPROVE_ONCE);

            TurnResult result = handle.completion().get(2, TimeUnit.SECONDS);
            assertEquals(TurnOutcome.COMPLETED, result.outcome());
            assertEquals("ok", observer.toolResults.get(0).content());
            assertEquals(1, tool.executions.size());
        }
    }

    @Test
    void deniedToolDoesNotExecute() throws Exception {
        FakeProvider provider = new FakeProvider(List.of(
                new FakeProvider.Turn(List.of(), List.of(), List.of(TestMessages.call("call-1"))),
                new FakeProvider.Turn(List.of("done"), List.of(), List.of())));
        FakeTool tool = new FakeTool("mutate", ToolRisk.MUTATING, "ok");
        try (AgentRuntime runtime =
                AgentRuntimeTestSupport.runtime(AgentRuntimeTestSupport.root(), provider, List.of(tool))) {
            Conversation conversation = runtime.createConversation("Chat", "main");
            RecordingObserver observer = new RecordingObserver();
            TurnHandle handle = runtime.startTurn(conversation.id(), "do it", observer);
            observer.awaitApproval();
            runtime.approve(conversation.id(), observer.approval().id(), ApprovalChoice.DENY);

            assertEquals(
                    TurnOutcome.COMPLETED,
                    handle.completion().get(2, TimeUnit.SECONDS).outcome());
            assertTrue(observer.toolResults.get(0).error());
            assertTrue(tool.executions.isEmpty());
        }
    }

    @Test
    void toolExceptionBecomesErrorResultAndLoopContinues() throws Exception {
        FakeProvider provider = new FakeProvider(List.of(
                new FakeProvider.Turn(List.of(), List.of(), List.of(TestMessages.call("call-1"))),
                new FakeProvider.Turn(List.of("done"), List.of(), List.of())));
        FakeTool tool = new FakeTool("mutate", ToolRisk.READ_ONLY, "unused");
        tool.failWith(new IllegalStateException("tool boom"));
        try (AgentRuntime runtime =
                AgentRuntimeTestSupport.runtime(AgentRuntimeTestSupport.root(), provider, List.of(tool))) {
            Conversation conversation = runtime.createConversation("Chat", "main");
            RecordingObserver observer = new RecordingObserver();
            TurnResult result = runtime.startTurn(conversation.id(), "do it", observer)
                    .completion()
                    .get(2, TimeUnit.SECONDS);

            assertEquals(TurnOutcome.COMPLETED, result.outcome());
            assertTrue(observer.toolResults.get(0).error());
            assertEquals("done", observer.text.toString());
            assertEquals(1, tool.executions.size());
        }
    }

    @Test
    void unknownToolBecomesErrorResultAndLoopContinues() throws Exception {
        FakeProvider provider = new FakeProvider(List.of(
                new FakeProvider.Turn(
                        List.of(), List.of(), List.of(new ToolCall("call-1", "missing", new JsonObject()))),
                new FakeProvider.Turn(List.of("done"), List.of(), List.of())));
        try (AgentRuntime runtime =
                AgentRuntimeTestSupport.runtime(AgentRuntimeTestSupport.root(), provider, List.of())) {
            Conversation conversation = runtime.createConversation("Chat", "main");
            RecordingObserver observer = new RecordingObserver();
            TurnResult result = runtime.startTurn(conversation.id(), "do it", observer)
                    .completion()
                    .get(2, TimeUnit.SECONDS);

            assertEquals(TurnOutcome.COMPLETED, result.outcome());
            assertTrue(observer.toolResults.get(0).error());
            assertTrue(observer.toolResults.get(0).content().contains("Unknown tool"));
            assertEquals("done", observer.text.toString());
        }
    }

    @Test
    void stopsAtMaxToolRounds() throws Exception {
        List<FakeProvider.Step> steps = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            steps.add(new FakeProvider.Turn(List.of(), List.of(), List.of(TestMessages.call("call-" + index))));
        }
        FakeTool tool = new FakeTool("mutate", ToolRisk.READ_ONLY, "ok");
        try (AgentRuntime runtime = AgentRuntimeTestSupport.runtime(
                AgentRuntimeTestSupport.root(), new FakeProvider(steps), List.of(tool))) {
            Conversation conversation = runtime.createConversation("Chat", "main");
            RecordingObserver observer = new RecordingObserver();
            TurnResult result = runtime.startTurn(conversation.id(), "loop", observer)
                    .completion()
                    .get(3, TimeUnit.SECONDS);

            assertEquals(TurnOutcome.MAX_ITERATIONS, result.outcome());
            assertEquals(8, tool.executions.size());
        }
    }

    @Test
    void cancelStopsPendingApproval() throws Exception {
        FakeProvider provider = new FakeProvider(List.of(
                new FakeProvider.Turn(List.of(), List.of(), List.of(TestMessages.call("call-1"))),
                new FakeProvider.Turn(List.of("done"), List.of(), List.of())));
        FakeTool tool = new FakeTool("mutate", ToolRisk.MUTATING, "ok");
        try (AgentRuntime runtime =
                AgentRuntimeTestSupport.runtime(AgentRuntimeTestSupport.root(), provider, List.of(tool))) {
            Conversation conversation = runtime.createConversation("Chat", "main");
            RecordingObserver observer = new RecordingObserver();
            TurnHandle handle = runtime.startTurn(conversation.id(), "do it", observer);
            observer.awaitApproval();
            handle.cancel();

            assertEquals(
                    TurnOutcome.CANCELLED,
                    handle.completion().get(2, TimeUnit.SECONDS).outcome());
            assertTrue(tool.executions.isEmpty());
        }
    }

    @Test
    void approvalTimeoutDenies() throws Exception {
        FakeProvider provider = new FakeProvider(List.of(
                new FakeProvider.Turn(List.of(), List.of(), List.of(TestMessages.call("call-1"))),
                new FakeProvider.Turn(List.of("done"), List.of(), List.of())));
        FakeTool tool = new FakeTool("mutate", ToolRisk.MUTATING, "ok");
        try (AgentRuntime runtime =
                AgentRuntimeTestSupport.runtime(AgentRuntimeTestSupport.root(), provider, List.of(tool))) {
            Conversation conversation = runtime.createConversation("Chat", "main");
            RecordingObserver observer = new RecordingObserver();
            TurnResult result = runtime.startTurn(conversation.id(), "do it", observer)
                    .completion()
                    .get(3, TimeUnit.SECONDS);

            assertEquals(TurnOutcome.COMPLETED, result.outcome());
            assertTrue(observer.toolResults.get(0).error());
            assertTrue(tool.executions.isEmpty());
        }
    }
}
