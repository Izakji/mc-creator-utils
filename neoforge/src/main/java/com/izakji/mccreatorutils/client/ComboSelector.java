package com.izakji.mccreatorutils.client;

import com.izakji.mccreatorutils.search.SearchFilter;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.chat.Component;

/**
 * Editable selector: type to filter candidates or use the dropdown arrow.
 *
 * @author Izakji
 */
public final class ComboSelector<T> extends Selector<T> {
    private final TextField input = new TextField();
    private List<T> allCandidates;
    private final Function<T, Component> labels;
    private Consumer<T> externalListener;
    private boolean updatingText;
    private String lastText = "";

    public ComboSelector(List<T> candidates, Function<T, Component> labels) {
        this.allCandidates = List.copyOf(candidates);
        this.labels = labels;
        super.setCandidateUIProvider(UIElementProvider.text(labels));
        super.setCandidates(allCandidates);
        super.setOnValueChanged(this::onValueChanged);

        preview.setVisible(false);
        input.setAnyString();
        input.textFieldStyle(
                style -> style.placeholder(Component.translatable("tool.mccreatorutils.combo.placeholder")));
        input.layout(layout -> layout.flex(1).height(12));
        input.setTextResponder(ignored -> filter());
        input.addEventListener(UIEvents.TICK, event -> {
            if (!input.getRawText().equals(lastText)) {
                filter();
            }
        });
        input.addEventListener(UIEvents.MOUSE_DOWN, event -> show());
        display.addChildAt(input, 0);
    }

    public ComboSelector<T> setAllCandidates(List<T> candidates) {
        allCandidates = List.copyOf(candidates);
        super.setCandidates(allCandidates);
        return this;
    }

    @Override
    public Selector<T> setSelected(T value) {
        super.setSelected(value);
        updateText(value);
        return this;
    }

    @Override
    public Selector<T> setSelected(T value, boolean notify) {
        super.setSelected(value, notify);
        updateText(value);
        return this;
    }

    @Override
    public Selector<T> setOnValueChanged(Consumer<T> consumer) {
        externalListener = consumer;
        return this;
    }

    private void onValueChanged(T value) {
        updateText(value);
        if (externalListener != null) {
            externalListener.accept(value);
        }
    }

    private void filter() {
        lastText = input.getRawText();
        if (updatingText) {
            return;
        }
        List<T> filtered = SearchFilter.filter(
                allCandidates, lastText, value -> labels.apply(value).getString());
        super.setCandidates(filtered);
        if (!lastText.isEmpty()) {
            show();
        }
    }

    private void updateText(T value) {
        String text = value == null ? "" : labels.apply(value).getString();
        updatingText = true;
        input.setText(text, false);
        lastText = text;
        updatingText = false;
    }
}
