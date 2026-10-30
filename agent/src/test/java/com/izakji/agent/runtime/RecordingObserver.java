package com.izakji.agent.runtime;

import com.izakji.agent.api.AgentException;
import com.izakji.agent.api.ToolCall;
import com.izakji.agent.api.ToolResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class RecordingObserver implements TurnObserver {
    final StringBuilder text = new StringBuilder();
    final StringBuilder reasoning = new StringBuilder();
    final List<AgentException> errors = new ArrayList<>();
    final List<ApprovalRequest> approvals = new ArrayList<>();
    final List<ToolResult> toolResults = new ArrayList<>();
    private final CountDownLatch approvalLatch = new CountDownLatch(1);
    private final CountDownLatch finishedLatch = new CountDownLatch(1);
    TurnResult finished;

    @Override
    public void onTextDelta(String delta) {
        text.append(delta);
    }

    @Override
    public void onReasoningDelta(String delta) {
        reasoning.append(delta);
    }

    @Override
    public void onToolStarted(ToolCall call) {}

    @Override
    public void onToolFinished(ToolResult result) {
        toolResults.add(result);
    }

    @Override
    public void onApprovalNeeded(ApprovalRequest request) {
        approvals.add(request);
        approvalLatch.countDown();
    }

    @Override
    public void onError(AgentException error) {
        errors.add(error);
    }

    @Override
    public void onFinished(TurnResult result) {
        finished = result;
        finishedLatch.countDown();
    }

    boolean awaitFinished() throws InterruptedException {
        return finishedLatch.await(2, TimeUnit.SECONDS);
    }

    ApprovalRequest approval() {
        return approvals.get(0);
    }

    boolean awaitApproval() throws InterruptedException {
        return approvalLatch.await(2, TimeUnit.SECONDS);
    }
}
