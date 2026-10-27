package com.izakji.mccreatorutils.client.registry;

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
    private final Selector<ResourceLocation> entrySelector = new Selector<>();
    private final Consumer<ResourceLocation> onSelectionChanged;
    private final boolean hasGroups;

    public RegistryPicker(
            RegistryKind kind, ResourceLocation initialSelection, Consumer<ResourceLocation> onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
        this.layout(layout ->
                layout.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(3));

        List<RegistryGroup> groups = RegistryCatalog.groups(kind);
        this.hasGroups = !groups.isEmpty();

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
            entrySelector.setCandidates(group == null ? List.of() : group.entries());
            entrySelector.setSelected(null, false);
            entrySelector.setActive(group != null && !group.entries().isEmpty());
            this.onSelectionChanged.accept(null);
        });
        entrySelector.setOnValueChanged(this.onSelectionChanged);

        addChildren(
                section(Component.translatable("tool.mccreatorutils.registry.mod"), modSelector),
                section(Component.translatable("tool.mccreatorutils.registry.entry"), entrySelector));
    }

    public ResourceLocation selectedEntry() {
        return entrySelector.getValue();
    }

    public boolean hasGroups() {
        return hasGroups;
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
