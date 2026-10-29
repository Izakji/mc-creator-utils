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
 * Editable selector: type in the field or use the dropdown arrow.
 *
 * @author Izakji
 */
public final class ComboSelector<T> extends UIElement {
    private final TextField input = new TextField();
    private final Selector<T> selector = new Selector<>();
    private final Function<T, Component> labels;
    private List<T> allCandidates;
    private Consumer<T> externalListener;
    private String lastText = "";

    public ComboSelector(List<T> candidates, Function<T, Component> labels) {
        this.allCandidates = List.copyOf(candidates);
        this.labels = labels;
        layout(layout -> layout.widthPercent(100)
                .height(16)
                .flexDirection(FlexDirection.ROW)
                .gapAll(1));

        input.setAnyString();
        input.textFieldStyle(
                style -> style.placeholder(Component.translatable("tool.mccreatorutils.combo.placeholder")));
        input.layout(layout -> layout.flex(1).height(16));
        input.setTextResponder(ignored -> filter());
        input.addEventListener(UIEvents.TICK, event -> {
            if (!input.getRawText().equals(lastText)) {
                filter();
            }
        });

        selector.setCandidateUIProvider(UIElementProvider.text(labels));
        selector.setCandidates(allCandidates);
        selector.setValue(null, false);
        selector.setOnValueChanged(this::onValueChanged);
        selector.display.layout(layout -> layout.widthPercent(100)
                .heightPercent(100)
                .paddingTop(0)
                .paddingBottom(0)
                .paddingLeft(4)
                .paddingRight(2));
        selector.layout(layout -> layout.flex(1).height(16));

        addChildren(input, selector);
    }

    public ComboSelector<T> setAllCandidates(List<T> candidates) {
        allCandidates = List.copyOf(candidates);
        selector.setCandidates(allCandidates);
        input.setText("", false);
        lastText = "";
        return this;
    }

    public ComboSelector<T> setSelected(T value) {
        selector.setSelected(value, false);
        return this;
    }

    public ComboSelector<T> setSelected(T value, boolean notify) {
        selector.setSelected(value, notify);
        return this;
    }

    public ComboSelector<T> setOnValueChanged(Consumer<T> consumer) {
        externalListener = consumer;
        return this;
    }

    public T getValue() {
        return selector.getValue();
    }

    public void hide() {
        selector.hide();
    }

    private void onValueChanged(T value) {
        if (externalListener != null) {
            externalListener.accept(value);
        }
    }

    private void filter() {
        String text = input.getRawText();
        if (text.equals(lastText)) {
            return;
        }
        lastText = text;
        List<T> filtered = SearchFilter.filter(
                allCandidates, text, value -> labels.apply(value).getString());
        selector.setCandidates(filtered);
    }
}
