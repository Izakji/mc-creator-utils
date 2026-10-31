package com.izakji.agent.runtime;

import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ContentPart;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class FakeTool implements AgentTool {
    private final String name;
    private final ToolRisk risk;
    private final String result;
    final List<ToolInvocation> executions = new ArrayList<>();
    final CompletableFuture<ToolResult> pending = new CompletableFuture<>();
    private final java.util.concurrent.CountDownLatch started = new java.util.concurrent.CountDownLatch(1);
    private RuntimeException failure;
    private boolean neverCompletes;
    private List<ContentPart> attachments = List.of();

    FakeTool(String name, ToolRisk risk, String result) {
        this.name = name;
        this.risk = risk;
        this.result = result;
    }

    void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    void neverCompletes() {
        this.neverCompletes = true;
    }

    void withAttachments(List<ContentPart> attachments) {
        this.attachments = List.copyOf(attachments);
    }

    boolean awaitExecution() throws InterruptedException {
        return started.await(2, java.util.concurrent.TimeUnit.SECONDS);
    }

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
        executions.add(invocation);
        started.countDown();
        if (neverCompletes) {
            return pending;
        }
        if (failure != null) {
            return CompletableFuture.failedFuture(failure);
        }
        return CompletableFuture.completedFuture(new ToolResult(invocation.callId(), result, false, attachments));
    }
}
