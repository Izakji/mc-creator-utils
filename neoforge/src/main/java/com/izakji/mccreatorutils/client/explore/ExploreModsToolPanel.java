package com.izakji.mccreatorutils.client.explore;

import com.izakji.mccreatorutils.client.ComboSelector;
import com.izakji.mccreatorutils.client.CommandStatusLabel;
import com.izakji.mccreatorutils.client.CreatorToolsScreen;
import com.izakji.mccreatorutils.client.browser.BrowserOpenResult;
import com.izakji.mccreatorutils.client.browser.BrowserWorkspace;
import com.izakji.mccreatorutils.explore.InstalledMod;
import com.izakji.mccreatorutils.explore.ModLabels;
import com.izakji.mccreatorutils.explore.ModPlatform;
import com.izakji.mccreatorutils.explore.ResolvedModPage;
import com.izakji.mccreatorutils.session.CreatorToolSession;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Explore installed mods and open their CurseForge or Modrinth page.
 *
 * @author Izakji
 */
public final class ExploreModsToolPanel extends UIElement {
    private final CreatorToolSession session = CreatorToolSession.instance();
    private final CommandStatusLabel status = new CommandStatusLabel();
    private final ComboSelector<InstalledMod> modSelector;
    private final Button openButton = new Button();
    private final boolean rinkuAvailable = BrowserWorkspace.instance().isAvailable();
    private boolean removed;

    public ExploreModsToolPanel() {
        this.layout(layout -> layout.widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(5));

        var description = new Label();
        description.setText(Component.translatable("tool.mccreatorutils.explore_mods.description"));
        addChild(description);

        if (!rinkuAvailable) {
            var notice = new Label();
            notice.setText(Component.translatable("notice.mccreatorutils.rinku_missing"));
            notice.textStyle(style -> style.textColor(ColorPattern.YELLOW.color));
            addChild(notice);
        }

        var installedMods = InstalledModCatalog.load();
        modSelector = new ComboSelector<>(installedMods, mod -> Component.literal(ModLabels.label(mod)));
        modSelector.setOnValueChanged(selected -> {
            session.setExploreModId(selected == null ? null : selected.modId());
            status.setText(Component.empty());
            updateOpenButton();
        });
        if (session.exploreModId() != null) {
            installedMods.stream()
                    .filter(mod -> mod.modId().equals(session.exploreModId()))
                    .findFirst()
                    .ifPresent(mod -> modSelector.setSelected(mod, false));
        }
        addChild(modSelector);

        openButton.setText(Component.translatable("tool.mccreatorutils.explore_mods.open"));
        openButton.layout(layout -> layout.widthPercent(100).height(16));
        openButton.setActive(false);
        openButton.setOnClick(event -> openSelected());
        addChild(openButton);

        addChild(status);
        updateOpenButton();
    }

    @Override
    protected void onRemoved() {
        removed = true;
        super.onRemoved();
    }

    private void updateOpenButton() {
        openButton.setActive(rinkuAvailable && modSelector.getValue() != null);
    }

    private void openSelected() {
        InstalledMod selected = modSelector.getValue();
        if (selected == null || !rinkuAvailable) {
            return;
        }

        openButton.setActive(false);
        status.showInfo(Component.translatable("status.mccreatorutils.looking_up"));
        ModLinkResolver.resolve(selected)
                .whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
                    if (removed) {
                        return;
                    }
                    updateOpenButton();
                    if (error != null) {
                        status.showError(Component.translatable("status.mccreatorutils.lookup_failed"));
                        return;
                    }
                    Optional<ResolvedModPage> page = result == null ? Optional.empty() : result;
                    if (page.isEmpty()) {
                        status.showError(Component.translatable("status.mccreatorutils.no_mod_page"));
                        return;
                    }
                    if (!(Minecraft.getInstance().screen instanceof CreatorToolsScreen)) {
                        return;
                    }
                    ResolvedModPage resolved = page.get();
                    status.showInfo(Component.translatable(
                            resolved.platform() == ModPlatform.CURSEFORGE
                                    ? "status.mccreatorutils.opening_curseforge"
                                    : "status.mccreatorutils.opening_modrinth"));
                    BrowserOpenResult openResult =
                            BrowserWorkspace.instance().openPage(resolved.uri().toString());
                    if (openResult == BrowserOpenResult.CAPACITY) {
                        status.showError(Component.translatable("screen.mccreatorutils.browser.tab_limit"));
                    } else if (openResult == BrowserOpenResult.UNAVAILABLE) {
                        status.showError(Component.translatable("screen.mccreatorutils.browser.rinku_unavailable"));
                    } else if (openResult == BrowserOpenResult.FAILED) {
                        status.showError(Component.translatable("screen.mccreatorutils.browser.open_failed"));
                    }
                }));
    }
}
