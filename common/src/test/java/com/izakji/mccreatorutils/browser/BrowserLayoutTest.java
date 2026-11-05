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

    @Test
    void spacesNavigationButtonsWithGaps() {
        var navBar = new BrowserGeometry(100, 132, 400, 20);
        assertEquals(102, BrowserLayout.navButtonLeft(navBar, 0));
        assertEquals(126, BrowserLayout.navButtonLeft(navBar, 1));
        assertEquals(150, BrowserLayout.navButtonLeft(navBar, 2));

        assertEquals(0, BrowserLayout.navButtonAt(navBar, 102));
        assertEquals(0, BrowserLayout.navButtonAt(navBar, 121.9));
        assertEquals(-1, BrowserLayout.navButtonAt(navBar, 122));
        assertEquals(1, BrowserLayout.navButtonAt(navBar, 126));
        assertEquals(-1, BrowserLayout.navButtonAt(navBar, 146));
        assertEquals(2, BrowserLayout.navButtonAt(navBar, 150));
        assertEquals(-1, BrowserLayout.navButtonAt(navBar, 170));
    }
}
