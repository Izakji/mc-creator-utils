package com.izakji.mccreatorutils.client.browser;

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
        return new RinkuBrowserTab(initialUrl);
    }
}
