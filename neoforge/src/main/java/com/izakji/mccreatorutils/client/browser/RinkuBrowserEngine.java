package com.izakji.mccreatorutils.client.browser;

import com.izakji.mccreatorutils.McCreatorUtils;
import de.keksuccino.rinku.Rinku;

/**
 * Creates Rinku browser tabs after Rinku has initialized.
 *
 * @author Izakji
 */
public final class RinkuBrowserEngine implements BrowserEngine {
    @Override
    public BrowserTabHandle create(String initialUrl) {
        if (!Rinku.isInitialized()) {
            throw new IllegalStateException("Rinku is not initialized");
        }
        installAdBlock();
        return new RinkuBrowserTab(initialUrl);
    }

    private static void installAdBlock() {
        if (!BrowserConfig.blockAds()) {
            return;
        }
        try {
            BrowserAdBlockList blockList = BrowserAdBlockList.load();
            Rinku.getClient().getHandle().addRequestHandler(new BrowserAdBlockHandler(blockList));
            McCreatorUtils.LOGGER.info("Browser ad blocker enabled with {} rules.", blockList.ruleCount());
        } catch (RuntimeException | LinkageError failure) {
            McCreatorUtils.LOGGER.warn("Could not install the browser ad blocker.", failure);
        }
    }
}
