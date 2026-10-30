package com.izakji.agent.runtime;

import com.google.gson.Gson;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.LlmProvider;
import com.izakji.agent.config.AgentSettings;
import com.izakji.agent.config.TestSettings;
import com.izakji.agent.conversation.FileConversationStore;
import com.izakji.agent.provider.ProviderRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AgentRuntimeTestSupport {
    private AgentRuntimeTestSupport() {}

    public static Path root() throws Exception {
        return Files.createTempDirectory("agent-runtime");
    }

    public static AgentSettings settings() {
        return TestSettings.settings();
    }

    public static AgentRuntime runtime(Path root, LlmProvider provider, List<AgentTool> tools) {
        return runtime(root, provider, tools, settings());
    }

    public static AgentRuntime runtime(Path root, LlmProvider provider, List<AgentTool> tools, AgentSettings settings) {
        ProviderRegistry providers = new ProviderRegistry();
        providers.register(provider);
        ToolRegistry registry = new ToolRegistry();
        tools.forEach(registry::register);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        return new AgentRuntime(
                settings,
                providers,
                registry,
                new FileConversationStore(root, new Gson()),
                new PermissionGate(settings.permissions(), List.of()),
                List.of(),
                "",
                executor,
                Clock.systemUTC());
    }
}
