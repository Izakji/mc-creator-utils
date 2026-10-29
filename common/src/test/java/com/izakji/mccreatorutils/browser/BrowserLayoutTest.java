package com.izakji.mccreatorutils.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BrowserLayoutTest {

    @Test
    void stacksChromeAndContent() {
        var layout = BrowserLayout.of(new BrowserGeometry(100, 100, 400, 300));
        assertEquals(new BrowserGeometry(100, 100, 400, 16), layout.titleBar());
        assertEquals(new BrowserGeometry(100, 116, 400, 16), layout.tabStrip());
        assertEquals(new BrowserGeometry(100, 132, 400, 20), layout.navBar());
        assertEquals(new BrowserGeometry(100, 152, 400, 16), layout.addressBar());
        assertEquals(new BrowserGeometry(102, 170, 396, 228), layout.content());
        assertEquals(new BrowserGeometry(488, 388, 12, 12), layout.resizeGrip());
    }
}
