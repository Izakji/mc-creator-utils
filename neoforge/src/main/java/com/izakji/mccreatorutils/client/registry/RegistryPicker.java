package com.izakji.mccreatorutils.client.registry;

import com.izakji.mccreatorutils.client.ComboSelector;
import com.izakji.mccreatorutils.registry.RegistryGroup;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared mod and registry-entry dropdowns for the browser tools.
 *
 * @author Izakji
 */
public final class RegistryPicker extends UIElement {
    private final Selector<RegistryGroup> modSelector = new Selector<>();
    private final Selector<ResourceLocation> entrySelector;
    private final ComboSelector<ResourceLocation> entryCombo;
    private final UIElement entryControl;
    private final Consumer<ResourceLocation> onSelectionChanged;
    private final List<RegistryGroup> groups;
    private final boolean hasGroups;

    public RegistryPicker(
            RegistryKind kind, ResourceLocation initialSelection, Consumer<ResourceLocation> onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
        this.layout(layout ->
                layout.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(3));

        groups = RegistryCatalog.groups(kind);
        this.hasGroups = !groups.isEmpty();
        boolean searchable = kind == RegistryKind.BIOME
                || kind == RegistryKind.STRUCTURE
                || kind == RegistryKind.ITEM
                || kind == RegistryKind.MOB;

        modSelector.setCandidates(groups);
        modSelector.setCandidateUIProvider(UIElementProvider.text(group -> group == null
                ? Component.translatable("tool.mccreatorutils.registry.mod.select")
                : RegistryCatalog.modName(group.namespace())));
        modSelector.setValue(null, false);
        modSelector.layout(layout -> layout.widthPercent(100).height(14));

        if (searchable) {
            entryCombo = new ComboSelector<>(
                    List.of(),
                    entry -> entry == null
                            ? Component.translatable("tool.mccreatorutils.registry.entry.select")
                            : RegistryCatalog.label(kind, entry));
            entrySelector = null;
            entryControl = entryCombo;
        } else {
            entryCombo = null;
            entrySelector = new Selector<>();
            entrySelector.setCandidateUIProvider(UIElementProvider.text(entry -> entry == null
                    ? Component.translatable("tool.mccreatorutils.registry.entry.select")
                    : RegistryCatalog.label(kind, entry)));
            entrySelector.setValue(null, false);
            entrySelector.layout(layout -> layout.widthPercent(100).height(14));
            entryControl = entrySelector;
        }
        setEntryActive(false);
        setEntryListener(this.onSelectionChanged);

        restore(initialSelection, groups);

        modSelector.setOnValueChanged(group -> {
            setEntryCandidates(group == null ? List.of() : group.entries());
            setEntrySelected(null, false);
            setEntryActive(group != null && !group.entries().isEmpty());
            this.onSelectionChanged.accept(null);
        });

        addChildren(
                section(Component.translatable("tool.mccreatorutils.registry.mod"), modSelector),
                section(Component.translatable("tool.mccreatorutils.registry.entry"), entryControl));
    }

    public ResourceLocation selectedEntry() {
        return entryCombo != null ? entryCombo.getValue() : entrySelector.getValue();
    }

    public boolean hasGroups() {
        return hasGroups;
    }

    private void setEntryCandidates(List<ResourceLocation> entries) {
        if (entryCombo != null) {
            entryCombo.setAllCandidates(entries);
        } else {
            entrySelector.setCandidates(entries);
        }
    }

    private void setEntrySelected(ResourceLocation entry, boolean notify) {
        if (entryCombo != null) {
            entryCombo.setSelected(entry, notify);
        } else {
            entrySelector.setSelected(entry, notify);
        }
    }

    private void setEntryActive(boolean active) {
        if (entryCombo != null) {
            entryCombo.setActive(active);
        } else {
            entrySelector.setActive(active);
        }
    }

    private void setEntryListener(Consumer<ResourceLocation> listener) {
        if (entryCombo != null) {
            entryCombo.setOnValueChanged(listener);
        } else {
            entrySelector.setOnValueChanged(listener);
        }
    }

    private void restore(ResourceLocation initialSelection, List<RegistryGroup> groups) {
        if (initialSelection == null) {
            return;
        }
        RegistryGroup group = groups.stream()
                .filter(candidate -> candidate.namespace().equals(initialSelection.getNamespace()))
                .findFirst()
                .orElse(null);
        if (group == null || !group.entries().contains(initialSelection)) {
            return;
        }
        modSelector.setSelected(group, false);
        setEntryCandidates(group.entries());
        setEntryActive(true);
        setEntrySelected(initialSelection, false);
    }

    @Override
    protected void onRemoved() {
        modSelector.hide();
        if (entryCombo != null) {
            entryCombo.hide();
        } else {
            entrySelector.hide();
        }
        super.onRemoved();
    }

    private static UIElement section(Component labelText, UIElement selector) {
        var section = new UIElement();
        section.layout(layout ->
                layout.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(2));
        var label = new Label();
        label.setText(labelText);
        section.addChildren(label, selector);
        return section;
    }
}
