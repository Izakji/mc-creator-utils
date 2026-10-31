package com.izakji.mccreatorutils.client.agent;

import com.izakji.agent.config.ProviderPresets;
import com.izakji.agent.config.ProviderProfile;
import com.izakji.agent.config.ProviderType;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Provider profile editor and connection tester.
 *
 * @author Izakji
 */
public final class AgentProviderTab extends UIElement {
    private static final List<ProviderProfile> PRESETS = List.of(
            ProviderPresets.openAi(""),
            ProviderPresets.openRouter(""),
            ProviderPresets.deepSeek(""),
            ProviderPresets.groq(""),
            ProviderPresets.ollama(""),
            ProviderPresets.lmStudio(""),
            ProviderPresets.anthropic(""));

    private final AgentClientService service = AgentClientService.instance();
    private final Selector<ProviderProfile> profiles = new Selector<>();
    private final Selector<ProviderProfile> presets = new Selector<>();
    private final Selector<ProviderType> types = new Selector<>();
    private final TextField displayName = field("Display name");
    private final TextField baseUrl = field("Base URL");
    private final TextField model = field("Model");
    private final TextField apiKey = field("Leave blank to keep the existing key");
    private final TextField temperature = field("Temperature");
    private final TextField maxOutput = field("Max output");
    private final TextField contextWindow = field("Context window");
    private final TextField thinkingBudget = field("Thinking budget");
    private final Switch tools = new Switch();
    private final Label status = new Label();
    private String selectedId = "";
    private boolean deleteArmed;

    public AgentProviderTab() {
        this.layout(layout -> layout.widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(3));

        profiles.layout(layout -> layout.widthPercent(100).height(16));
        profiles.setCandidateUIProvider(UIElementProvider.text(
                profile -> profile == null ? Component.empty() : Component.literal(profile.displayName())));
        profiles.setOnValueChanged(profile -> {
            if (profile != null) {
                fill(profile);
            }
        });
        addChild(profiles);

        presets.layout(layout -> layout.widthPercent(100).height(16));
        presets.setCandidates(PRESETS);
        presets.setCandidateUIProvider(UIElementProvider.text(
                profile -> profile == null ? Component.empty() : Component.literal(profile.displayName())));
        presets.setOnValueChanged(profile -> {
            if (profile != null) {
                applyPreset(profile);
            }
        });
        addChild(presets);

        types.setCandidates(List.of(ProviderType.values()));
        types.setCandidateUIProvider(
                UIElementProvider.text(type -> type == null ? Component.empty() : Component.literal(typeLabel(type))));
        addRow("Provider type", types, 16);

        addRow("Display name", displayName, 14);
        addRow("Base URL", baseUrl, 14);
        addRow("Model", model, 14);
        addRow("API key", apiKey, 14);

        var numbers = new UIElement();
        numbers.layout(layout -> layout.widthPercent(100)
                .height(14)
                .flexDirection(FlexDirection.ROW)
                .gapAll(3));
        temperature.layout(layout -> layout.flex(1).height(14));
        maxOutput.layout(layout -> layout.flex(1).height(14));
        contextWindow.layout(layout -> layout.flex(1).height(14));
        thinkingBudget.layout(layout -> layout.flex(1).height(14));
        numbers.addChildren(temperature, maxOutput, contextWindow, thinkingBudget);
        addRow("Limits", numbers, 14);

        var toolsRow = new UIElement();
        toolsRow.layout(layout -> layout.widthPercent(100)
                .height(16)
                .flexDirection(FlexDirection.ROW)
                .gapAll(4));
        var toolsLabel = new Label();
        toolsLabel.setText(Component.literal("Tool calling"));
        toolsLabel.layout(layout -> layout.width(100).height(14));
        tools.layout(layout -> layout.width(30).height(14));
        toolsRow.addChildren(toolsLabel, tools);
        addChild(toolsRow);

        var buttons = new UIElement();
        buttons.layout(layout -> layout.widthPercent(100)
                .height(16)
                .flexDirection(FlexDirection.ROW)
                .gapAll(3));
        var newButton = button("Add Profile", this::newProfile);
        var saveButton = button("Save", this::save);
        var deleteButton = button("Delete", this::delete);
        var testButton = button("Test Connection", this::test);
        buttons.addChildren(newButton, saveButton, deleteButton, testButton);
        addChild(buttons);

        status.layout(layout -> layout.widthPercent(100).height(12));
        addChild(status);

        refreshProfiles();
        if (!service.settings().profiles().isEmpty()) {
            fill(service.settings().profiles().get(0));
        } else {
            applyPreset(PRESETS.get(0));
        }
    }

    private static TextField field(String placeholder) {
        var field = new TextField();
        field.layout(layout -> layout.flex(1).height(14));
        field.textFieldStyle(style -> style.placeholder(Component.literal(placeholder)));
        return field;
    }

    private static Button button(String label, Runnable action) {
        var button = new Button();
        button.setText(Component.literal(label));
        button.layout(layout -> layout.flex(1).height(16));
        button.setOnClick(event -> action.run());
        return button;
    }

    private void addRow(String label, UIElement field, int height) {
        var row = new UIElement();
        row.layout(layout -> layout.widthPercent(100)
                .height(height)
                .flexDirection(FlexDirection.ROW)
                .gapAll(4));
        var text = new Label();
        text.setText(Component.literal(label));
        text.layout(layout -> layout.width(92).height(height));
        field.layout(layout -> layout.flex(1).height(height));
        row.addChildren(text, field);
        addChild(row);
    }

    private void refreshProfiles() {
        List<ProviderProfile> loaded = service.settings().profiles();
        profiles.setCandidates(loaded);
        if (!selectedId.isBlank()) {
            loaded.stream()
                    .filter(profile -> profile.id().equals(selectedId))
                    .findFirst()
                    .ifPresent(profiles::setSelected);
        } else if (!loaded.isEmpty()) {
            profiles.setSelected(loaded.get(0));
        }
    }

    private void fill(ProviderProfile profile) {
        selectedId = profile.id();
        displayName.setText(profile.displayName());
        types.setSelected(profile.type());
        baseUrl.setText(profile.baseUrl());
        model.setText(profile.model());
        apiKey.setText("");
        temperature.setText(Double.toString(profile.temperature()));
        maxOutput.setText(Integer.toString(profile.maxOutputTokens()));
        contextWindow.setText(Integer.toString(profile.contextWindow()));
        thinkingBudget.setText(Integer.toString(profile.thinkingBudgetTokens()));
        tools.setOn(profile.tools(), false);
        deleteArmed = false;
    }

    private void applyPreset(ProviderProfile preset) {
        displayName.setText(preset.displayName());
        types.setSelected(preset.type());
        baseUrl.setText(preset.baseUrl());
        model.setText(preset.model());
        apiKey.setText("");
        temperature.setText(Double.toString(preset.temperature()));
        maxOutput.setText(Integer.toString(preset.maxOutputTokens()));
        contextWindow.setText(Integer.toString(preset.contextWindow()));
        thinkingBudget.setText(Integer.toString(preset.thinkingBudgetTokens()));
        tools.setOn(preset.tools(), false);
        deleteArmed = false;
    }

    private void newProfile() {
        selectedId = "";
        applyPreset(PRESETS.get(0));
        status.setText(Component.literal("New profile"));
        status.textStyle(style -> style.textColor(ColorPattern.YELLOW.color));
    }

    private void save() {
        deleteArmed = false;
        AgentProviderDraft draft = draft();
        if (draft == null) {
            return;
        }
        List<String> errors = draft.validationErrors();
        if (!errors.isEmpty()) {
            showError(errors.get(0));
            return;
        }
        String id = selectedId.isBlank() ? UUID.randomUUID().toString() : selectedId;
        String existingKey = service.profile(id).map(ProviderProfile::apiKey).orElse("");
        AgentChatSession.instance().stop();
        service.upsertProfile(draft.toProfile(id, existingKey));
        selectedId = id;
        refreshProfiles();
        status.setText(Component.literal("Provider saved."));
        status.textStyle(style -> style.textColor(ColorPattern.GREEN.color));
    }

    private void test() {
        deleteArmed = false;
        AgentProviderDraft draft = draft();
        if (draft == null) {
            return;
        }
        List<String> errors = draft.validationErrors();
        if (!errors.isEmpty()) {
            showError(errors.get(0));
            return;
        }
        String id = selectedId.isBlank() ? "connection-test" : selectedId;
        String existingKey = service.profile(id).map(ProviderProfile::apiKey).orElse("");
        ProviderProfile profile = draft.toProfile(id, existingKey);
        status.setText(Component.literal("Testing connection..."));
        status.textStyle(style -> style.textColor(ColorPattern.YELLOW.color));
        new AgentConnectionTester()
                .test(service.providerFor(profile.type()), profile)
                .whenComplete((text, failure) -> Minecraft.getInstance().execute(() -> {
                    if (failure == null) {
                        status.setText(Component.literal("Connected: " + text));
                        status.textStyle(style -> style.textColor(ColorPattern.GREEN.color));
                    } else {
                        Throwable cause = failure.getCause() == null ? failure : failure.getCause();
                        showError(cause.getMessage() == null ? "Connection failed" : cause.getMessage());
                    }
                }));
    }

    private void delete() {
        if (selectedId.isBlank()) {
            return;
        }
        if (!deleteArmed) {
            deleteArmed = true;
            status.setText(Component.literal("Click Delete again to remove this profile."));
            status.textStyle(style -> style.textColor(ColorPattern.YELLOW.color));
            return;
        }
        deleteArmed = false;
        AgentChatSession.instance().stop();
        service.deleteProfile(selectedId);
        selectedId = "";
        refreshProfiles();
        if (!service.settings().profiles().isEmpty()) {
            fill(service.settings().profiles().get(0));
        } else {
            applyPreset(PRESETS.get(0));
        }
    }

    private AgentProviderDraft draft() {
        try {
            AgentProviderDraft draft = new AgentProviderDraft(
                    displayName.getRawText(),
                    types.getValue(),
                    baseUrl.getRawText(),
                    model.getRawText(),
                    apiKey.getRawText(),
                    Double.parseDouble(temperature.getRawText()),
                    Integer.parseInt(maxOutput.getRawText()),
                    Integer.parseInt(contextWindow.getRawText()),
                    tools.getValue(),
                    Integer.parseInt(thinkingBudget.getRawText()));
            return draft;
        } catch (NumberFormatException failure) {
            showError("Limits must be numbers.");
            return null;
        }
    }

    private void showError(String message) {
        status.setText(Component.literal(message));
        status.textStyle(style -> style.textColor(ColorPattern.RED.color));
    }

    private static String typeLabel(ProviderType type) {
        return switch (type) {
            case OPENAI_COMPATIBLE -> "OpenAI-compatible";
            case ANTHROPIC -> "Anthropic";
        };
    }
}
