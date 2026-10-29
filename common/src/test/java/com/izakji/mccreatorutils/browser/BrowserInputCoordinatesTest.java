package com.izakji.mccreatorutils.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BrowserInputCoordinatesTest {
    @Test
    void convertsGuiCoordinatesToBrowserTexturePixels() {
        assertEquals(96, BrowserInputCoordinates.toTexturePixel(50.0, 2, 2.0));
        assertEquals(0, BrowserInputCoordinates.toTexturePixel(2.0, 2, 2.0));
    }
}
