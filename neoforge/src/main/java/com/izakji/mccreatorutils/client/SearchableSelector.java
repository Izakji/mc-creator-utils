package com.izakji.mccreatorutils.client;

import com.izakji.mccreatorutils.search.SearchFilter;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.chat.Component;

/**
 * Selector with a search field above its candidate list.
 *
 * @author Izakji
 */
public final class SearchableSelector<T> extends UIElement {
    private final TextField search = new TextField();
    private final Selector<T> selector = new Selector<>();
    private List<T> candidates = List.of();
    private Function<T, Component> labels = value -> Component.empty();
    private Consumer<T> onValueChanged;
    private T selected;
    private String lastQuery = "";

    public SearchableSelector() {
        layout(layout ->
                layout.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(2));
        search.setAnyString();
        search.textFieldStyle(
                style -> style.placeholder(Component.translatable("tool.mccreatorutils.search.placeholder")));
        search.layout(layout -> layout.widthPercent(100).height(11));
        search.setTextResponder(ignored -> filter());
        search.addEventListener(UIEvents.TICK, event -> {
            if (!search.getRawText().equals(lastQuery)) {
                filter();
            }
        });
        selector.layout(layout -> layout.widthPercent(100).height(14));
        selector.setOnValueChanged(value -> {
            selected = value;
            if (onValueChanged != null) {
                onValueChanged.accept(value);
            }
        });
        addChildren(search, selector);
    }

    public SearchableSelector<T> setCandidates(List<T> candidates, Function<T, Component> labels) {
        this.candidates = List.copyOf(candidates);
        this.labels = labels;
        selector.setCandidateUIProvider(UIElementProvider.text(labels));
        filter();
        return this;
    }

    public SearchableSelector<T> setSelected(T value) {
        selected = value;
        selector.setSelected(value, false);
        return this;
    }

    public SearchableSelector<T> setOnValueChanged(Consumer<T> onValueChanged) {
        this.onValueChanged = onValueChanged;
        return this;
    }

    public T getValue() {
        return selector.getValue();
    }

    private void filter() {
        lastQuery = search.getRawText();
        List<T> filtered = SearchFilter.filter(
                candidates, lastQuery, value -> labels.apply(value).getString());
        selector.setCandidates(filtered);
        if (selected == null || !filtered.contains(selected)) {
            T first = filtered.isEmpty() ? null : filtered.get(0);
            selected = first;
            selector.setSelected(first, false);
            if (onValueChanged != null) {
                onValueChanged.accept(first);
            }
        }
    }
}
