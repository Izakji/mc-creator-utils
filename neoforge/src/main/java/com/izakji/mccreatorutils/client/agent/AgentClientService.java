package com.izakji.mccreatorutils.client.agent;

import com.google.gson.Gson;
import com.izakji.agent.api.AgentException;
import com.izakji.agent.api.LlmProvider;
import com.izakji.agent.config.AgentSettings;
import com.izakji.agent.config.AgentSettingsStore;
import com.izakji.agent.config.ProviderProfile;
import com.izakji.agent.config.ProviderType;
import com.izakji.agent.conversation.Conversation;
import com.izakji.agent.conversation.ConversationStore;
import com.izakji.agent.conversation.ConversationSummary;
import com.izakji.agent.conversation.FileConversationStore;
import com.izakji.agent.provider.AnthropicProvider;
import com.izakji.agent.provider.OpenAiCompatibleProvider;
import com.izakji.agent.provider.ProviderRegistry;
import com.izakji.agent.runtime.AgentRuntime;
import com.izakji.agent.runtime.ApprovalChoice;
import com.izakji.agent.runtime.InstructionsLoader;
import com.izakji.agent.runtime.PermissionGate;
import com.izakji.agent.runtime.ToolRegistry;
import com.izakji.agent.runtime.TurnHandle;
import com.izakji.agent.runtime.TurnObserver;
import com.izakji.agent.skill.LoadSkillTool;
import com.izakji.agent.skill.Skill;
import com.izakji.agent.skill.SkillLoader;
import com.izakji.agent.skill.SkillRegistry;
import com.izakji.mccreatorutils.client.agent.context.BlockInfoTool;
import com.izakji.mccreatorutils.client.agent.context.ClientLogReader;
import com.izakji.mccreatorutils.client.agent.context.DebugInfoTool;
import com.izakji.mccreatorutils.client.agent.context.EntityInfoTool;
import com.izakji.mccreatorutils.client.agent.context.GameSnapshotProvider;
import com.izakji.mccreatorutils.client.agent.context.GameSnapshotTool;
import com.izakji.mccreatorutils.client.agent.context.InventoryTool;
import com.izakji.mccreatorutils.client.agent.context.LogSummaryTool;
import com.izakji.mccreatorutils.client.agent.context.LogsTailTool;
import com.izakji.mccreatorutils.client.agent.context.ModListTool;
import com.izakji.mccreatorutils.client.agent.context.NetworkStatsTool;
import com.izakji.mccreatorutils.client.agent.context.RecipeLookupTool;
import com.izakji.mccreatorutils.client.agent.context.RegistrySearchTool;
import com.izakji.mccreatorutils.client.agent.context.ScreenshotTool;
import com.izakji.mccreatorutils.client.agent.context.SearchLogsTool;
import com.izakji.mccreatorutils.client.agent.context.ShowPerformanceReportTool;
import com.izakji.mccreatorutils.client.agent.context.SystemStatsTool;
import com.izakji.mccreatorutils.client.agent.context.TickStatsTool;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Client-side owner of agent settings, conversations, and the runtime.
 *
 * @author Izakji
 */
public final class AgentClientService implements AutoCloseable, AgentTurnPort {
    private static volatile AgentClientService instance;

    private final AgentPaths paths;
    private final AgentSettingsStore settingsStore = new AgentSettingsStore();
    private final ProviderRegistry providers = new ProviderRegistry();
    private final ConversationStore conversations;
    private final InstructionsLoader instructionsLoader = new InstructionsLoader();
    private final GameSnapshotProvider gameSnapshotProvider = new GameSnapshotProvider();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(daemonThreads("io"));
    private final ClientLogReader logReader;
    private final Path gameDirectory;
    private AgentSettings settings;
    private List<Skill> skills = List.of();
    private String instructions = "";
    private String startupWarning = "";
    private AgentRuntime runtime;

    private AgentClientService(Path configDir) {
        this.paths = AgentPaths.at(configDir);
        Path parent = configDir.toAbsolutePath().getParent();
        this.gameDirectory = parent == null ? configDir.toAbsolutePath() : parent;
        this.logReader = new ClientLogReader(gameDirectory.resolve("logs").resolve("latest.log"), ioExecutor);
        try {
            paths.ensureDirectories();
        } catch (IOException failure) {
            throw new IllegalStateException("Could not create agent directories", failure);
        }
        this.settings = loadSettings();
        this.conversations = new FileConversationStore(paths.conversationsDir(), new Gson());
        providers.register(new OpenAiCompatibleProvider());
        providers.register(new AnthropicProvider());
        SkillRegistry registry = new SkillRegistry();
        this.skills = loadSkills(registry);
        this.instructions = loadInstructions();
        this.runtime = buildRuntime(registry);
    }

    /**
     * Creates a service rooted at a config directory.
     *
     * @param configDir Minecraft config directory
     * @return initialized service
     */
    public static AgentClientService forConfig(Path configDir) {
        return new AgentClientService(configDir);
    }

    /**
     * Returns the shared client service, creating it lazily.
     *
     * @return shared service
     */
    public static AgentClientService instance() {
        AgentClientService current = instance;
        if (current == null) {
            synchronized (AgentClientService.class) {
                current = instance;
                if (current == null) {
                    current = forConfig(FMLPaths.CONFIGDIR.get());
                    instance = current;
                }
            }
        }
        return current;
    }

    public AgentSettings settings() {
        return settings;
    }

    public AgentPaths paths() {
        return paths;
    }

    public ProviderRegistry providers() {
        return providers;
    }

    public ExecutorService ioExecutor() {
        return ioExecutor;
    }

    public ClientLogReader logReader() {
        return logReader;
    }

    public String startupWarning() {
        return startupWarning;
    }

    public List<Skill> skills() {
        return skills;
    }

    public String instructions() {
        return instructions;
    }

    public AgentRuntime runtime() {
        return runtime;
    }

    public ConversationStore conversations() {
        return conversations;
    }

    public List<ConversationSummary> listConversations() {
        return conversations.list();
    }

    public Optional<Conversation> conversation(String id) {
        return conversations.load(id);
    }

    public Optional<ProviderProfile> profile(String id) {
        return settings.profiles().stream()
                .filter(profile -> profile.id().equals(id))
                .findFirst();
    }

    /**
     * Adds or replaces a provider profile and persists the settings.
     *
     * @param profile new profile
     */
    public void upsertProfile(ProviderProfile profile) {
        List<ProviderProfile> profiles = new java.util.ArrayList<>(settings.profiles());
        boolean replaced = false;
        for (int index = 0; index < profiles.size(); index++) {
            if (profiles.get(index).id().equals(profile.id())) {
                profiles.set(index, profile);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            profiles.add(profile);
        }
        String defaultId = settings.defaultProfileId();
        String selectedDefault = defaultId;
        if (selectedDefault.isBlank()
                || profiles.stream().noneMatch(item -> item.id().equals(selectedDefault))) {
            defaultId = profile.id();
        }
        saveSettings(new AgentSettings(
                defaultId,
                profiles,
                settings.permissions(),
                settings.maxToolRounds(),
                settings.maxToolCalls(),
                settings.approvalTimeoutSeconds()));
    }

    /**
     * Deletes a profile and chooses a remaining profile as default.
     *
     * @param id profile id
     */
    public void deleteProfile(String id) {
        List<ProviderProfile> profiles = settings.profiles().stream()
                .filter(profile -> !profile.id().equals(id))
                .toList();
        String defaultId = settings.defaultProfileId();
        String selectedDefault = defaultId;
        if (selectedDefault.equals(id)
                || profiles.stream().noneMatch(profile -> profile.id().equals(selectedDefault))) {
            defaultId = profiles.isEmpty() ? "" : profiles.get(0).id();
        }
        saveSettings(new AgentSettings(
                defaultId,
                profiles,
                settings.permissions(),
                settings.maxToolRounds(),
                settings.maxToolCalls(),
                settings.approvalTimeoutSeconds()));
    }

    public LlmProvider providerFor(ProviderType type) {
        return providers
                .forType(type)
                .orElseThrow(() -> new IllegalArgumentException("Provider not registered: " + type));
    }

    public Conversation createConversation(String profileId) {
        return createConversation(profileId, "New Chat");
    }

    public Conversation createConversation(String profileId, String title) {
        Conversation conversation = new Conversation(
                java.util.UUID.randomUUID().toString(),
                title,
                java.time.Instant.now(),
                java.time.Instant.now(),
                profileId,
                List.of(),
                java.util.Set.of());
        conversations.save(conversation);
        return conversation;
    }

    public void renameConversation(String id, String title) {
        conversations.rename(id, title);
    }

    public void deleteConversation(String id) {
        conversations.delete(id);
    }

    public TurnHandle startTurn(String conversationId, String userText, TurnObserver observer) {
        return runtime.startTurn(conversationId, userText, observer);
    }

    public void approve(String conversationId, String approvalId, ApprovalChoice choice) {
        runtime.approve(conversationId, approvalId, choice);
    }

    /**
     * Persists settings and rebuilds the runtime.
     *
     * @param updated new settings
     */
    public void saveSettings(AgentSettings updated) {
        try {
            settingsStore.save(paths.settingsFile(), updated);
        } catch (AgentException failure) {
            throw new IllegalStateException(failure.getMessage(), failure);
        }
        settings = updated;
        reloadExtensions();
    }

    /**
     * Reloads skills and AGENTS.md, then rebuilds the runtime.
     */
    public void reloadExtensions() {
        SkillRegistry registry = new SkillRegistry();
        skills = loadSkills(registry);
        instructions = loadInstructions();
        AgentRuntime previous = runtime;
        runtime = buildRuntime(registry);
        if (previous != null) {
            previous.close();
        }
    }

    @Override
    public void close() {
        AgentRuntime current = runtime;
        runtime = null;
        if (current != null) {
            current.close();
        }
        ioExecutor.shutdownNow();
        if (instance == this) {
            instance = null;
        }
    }

    private AgentSettings loadSettings() {
        if (!Files.isRegularFile(paths.settingsFile())) {
            AgentSettings defaults = AgentDefaults.settings();
            try {
                settingsStore.save(paths.settingsFile(), defaults);
            } catch (AgentException failure) {
                throw new IllegalStateException(failure.getMessage(), failure);
            }
            return defaults;
        }
        try {
            return settingsStore.load(paths.settingsFile());
        } catch (AgentException failure) {
            quarantineSettings();
            AgentSettings defaults = AgentDefaults.settings();
            try {
                settingsStore.save(paths.settingsFile(), defaults);
            } catch (AgentException saveFailure) {
                throw new IllegalStateException(saveFailure.getMessage(), saveFailure);
            }
            startupWarning = "Agent settings were unreadable; defaults were loaded.";
            return defaults;
        }
    }

    private void quarantineSettings() {
        try {
            Files.move(
                    paths.settingsFile(),
                    paths.settingsFile().resolveSibling("agent.json.corrupt-" + System.currentTimeMillis()),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // Fall back to defaults even when the corrupt file cannot be moved.
        }
    }

    private AgentRuntime buildRuntime(SkillRegistry registry) {
        ToolRegistry tools = new ToolRegistry();
        tools.register(new LoadSkillTool(registry));
        tools.register(new GameSnapshotTool(gameSnapshotProvider));
        tools.register(new TickStatsTool());
        tools.register(new SystemStatsTool());
        tools.register(new DebugInfoTool());
        tools.register(new NetworkStatsTool());
        tools.register(new LogsTailTool(logReader));
        tools.register(new SearchLogsTool(logReader));
        tools.register(new LogSummaryTool(logReader));
        tools.register(new ShowPerformanceReportTool(logReader));
        tools.register(new ScreenshotTool(gameDirectory, ioExecutor));
        tools.register(new ModListTool());
        tools.register(new InventoryTool());
        tools.register(new BlockInfoTool());
        tools.register(new EntityInfoTool());
        tools.register(new RegistrySearchTool());
        tools.register(new RecipeLookupTool());
        return new AgentRuntime(
                settings,
                providers,
                tools,
                conversations,
                new PermissionGate(settings.permissions(), List.of()),
                List.of(gameSnapshotProvider),
                instructions,
                skills,
                List.of(),
                Executors.newFixedThreadPool(2, daemonThreads("runtime")),
                Clock.systemUTC());
    }

    private List<Skill> loadSkills(SkillRegistry registry) {
        try {
            List<Skill> loaded = new SkillLoader().load(paths.skillsDir());
            loaded.forEach(registry::register);
            return loaded;
        } catch (IOException | IllegalArgumentException failure) {
            return List.of();
        }
    }

    private String loadInstructions() {
        try {
            return instructionsLoader.load(paths.agentsMarkdown());
        } catch (AgentException failure) {
            return "";
        }
    }

    private static ThreadFactory daemonThreads(String kind) {
        AtomicInteger next = new AtomicInteger();
        return task -> {
            Thread thread = new Thread(task, "creator-utils-agent-" + kind + "-" + next.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
