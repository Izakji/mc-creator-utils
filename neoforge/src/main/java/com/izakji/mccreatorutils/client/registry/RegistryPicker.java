package com.izakji.mccreatorutils.client.registry;

import com.izakji.mccreatorutils.registry.RegistryGroup;
import com.izakji.mccreatorutils.search.SearchFilter;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.ArrayList;
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
    private final Selector<ResourceLocation> entrySelector = new Selector<>();
    private final TextField searchField = new TextField();
    private final Consumer<ResourceLocation> onSelectionChanged;
    private final RegistryKind kind;
    private final List<RegistryGroup> groups;
    private final boolean hasGroups;
    private String lastQuery = "";

    public RegistryPicker(
            RegistryKind kind, ResourceLocation initialSelection, Consumer<ResourceLocation> onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
        this.kind = kind;
        this.layout(layout ->
                layout.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(3));

        groups = RegistryCatalog.groups(kind);
        this.hasGroups = !groups.isEmpty();

        searchField.setAnyString();
        searchField.textFieldStyle(
                style -> style.placeholder(Component.translatable("tool.mccreatorutils.search.placeholder")));
        searchField.layout(layout -> layout.widthPercent(100).height(11));
        searchField.setTextResponder(ignored -> applySearch());
        searchField.addEventListener(UIEvents.TICK, event -> {
            if (!searchField.getRawText().equals(lastQuery)) {
                applySearch();
            }
        });

        modSelector.setCandidates(groups);
        modSelector.setCandidateUIProvider(UIElementProvider.text(group -> group == null
                ? Component.translatable("tool.mccreatorutils.registry.mod.select")
                : RegistryCatalog.modName(group.namespace())));
        modSelector.layout(layout -> layout.widthPercent(100).height(14));

        entrySelector.setCandidateUIProvider(UIElementProvider.text(entry -> entry == null
                ? Component.translatable("tool.mccreatorutils.registry.entry.select")
                : RegistryCatalog.label(kind, entry)));
        entrySelector.layout(layout -> layout.widthPercent(100).height(14));
        entrySelector.setActive(false);

        restore(initialSelection, groups);

        modSelector.setOnValueChanged(group -> {
            List<ResourceLocation> entries = group == null ? List.of() : filterEntries(group);
            entrySelector.setCandidates(entries);
            entrySelector.setSelected(null, false);
            entrySelector.setActive(group != null && !entries.isEmpty());
            this.onSelectionChanged.accept(null);
        });
        entrySelector.setOnValueChanged(this.onSelectionChanged);

        addChildren(
                searchField,
                section(Component.translatable("tool.mccreatorutils.registry.mod"), modSelector),
                section(Component.translatable("tool.mccreatorutils.registry.entry"), entrySelector));
    }

    public ResourceLocation selectedEntry() {
        return entrySelector.getValue();
    }

    public boolean hasGroups() {
        return hasGroups;
    }

    private void applySearch() {
        String query = searchField.getRawText();
        lastQuery = query;
        List<RegistryGroup> filteredGroups = new ArrayList<>();
        for (RegistryGroup group : groups) {
            boolean modMatch =
                    matches(RegistryCatalog.modName(group.namespace()).getString(), query);
            List<ResourceLocation> entries = group.entries().stream()
                    .filter(entry -> query.isBlank()
                            || modMatch
                            || matches(entry.toString(), query)
                            || matches(RegistryCatalog.label(kind, entry).getString(), query))
                    .toList();
            if (!entries.isEmpty()) {
                filteredGroups.add(new RegistryGroup(group.namespace(), entries));
            }
        }
        if (query.isBlank()) {
            filteredGroups = groups;
        }

        RegistryGroup previous = modSelector.getValue();
        List<RegistryGroup> effectiveGroups = filteredGroups;
        modSelector.setCandidates(effectiveGroups);
        RegistryGroup selectedGroup = effectiveGroups.stream()
                .filter(group -> previous != null && group.namespace().equals(previous.namespace()))
                .findFirst()
                .orElseGet(() -> effectiveGroups.isEmpty() ? null : effectiveGroups.get(0));
        if (selectedGroup == null) {
            modSelector.setSelected(null, false);
            entrySelector.setCandidates(List.of());
            entrySelector.setActive(false);
            entrySelector.setSelected(null, false);
            onSelectionChanged.accept(null);
            return;
        }

        modSelector.setSelected(selectedGroup, false);
        entrySelector.setCandidates(selectedGroup.entries());
        entrySelector.setActive(true);
        if (!query.isBlank()) {
            ResourceLocation first = selectedGroup.entries().get(0);
            entrySelector.setSelected(first, false);
            onSelectionChanged.accept(first);
        } else {
            ResourceLocation current = entrySelector.getValue();
            if (current == null || !selectedGroup.entries().contains(current)) {
                entrySelector.setSelected(null, false);
                onSelectionChanged.accept(null);
            }
        }
    }

    private List<ResourceLocation> filterEntries(RegistryGroup group) {
        String query = searchField.getRawText();
        if (query.isBlank()
                || matches(RegistryCatalog.modName(group.namespace()).getString(), query)) {
            return group.entries();
        }
        return SearchFilter.filter(
                group.entries(),
                query,
                entry -> entry + " " + RegistryCatalog.label(kind, entry).getString());
    }

    private static boolean matches(String value, String query) {
        return query.isBlank() || value.toLowerCase().contains(query.trim().toLowerCase());
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
        entrySelector.setCandidates(group.entries());
        entrySelector.setActive(true);
        entrySelector.setSelected(initialSelection, false);
    }

    @Override
    protected void onRemoved() {
        modSelector.hide();
        entrySelector.hide();
        searchField.setVisible(false);
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
