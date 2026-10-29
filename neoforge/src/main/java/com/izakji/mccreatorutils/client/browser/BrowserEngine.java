package com.izakji.mccreatorutils.client.browser;

/**
 * Creates browser tab handles without exposing the browser backend to workspace UI code.
 *
 * @author Izakji
 */
public interface BrowserEngine {
    BrowserTabHandle create(String initialUrl);
}
