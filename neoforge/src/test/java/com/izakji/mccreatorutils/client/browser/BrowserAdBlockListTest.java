package com.izakji.mccreatorutils.client.browser;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BrowserAdBlockListTest {

    @Test
    void blocksKnownAdNetworksAndPaths() {
        BrowserAdBlockList list = new BrowserAdBlockList(List.of());
        assertTrue(list.matches("https://securepubads.g.doubleclick.net/tag/js/gpt.js"));
        assertTrue(list.matches("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"));
        assertTrue(list.matches("https://example.com/prebid/auction"));
        assertFalse(list.matches("https://www.curseforge.com/minecraft/mc-mods/jei"));
    }

    @Test
    void acceptsUserPatterns() {
        BrowserAdBlockList list = new BrowserAdBlockList(List.of("# comment", "ads.example.test"));
        assertTrue(list.matches("https://ads.example.test/banner.png"));
        assertFalse(list.matches("https://www.curseforge.com/"));
    }
}
