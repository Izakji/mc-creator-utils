package com.izakji.mccreatorutils.client.agent.context;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CurseForgeClientTest {

    @Test
    void reportsMissingApiKeyWithoutNetworkCalls() {
        CurseForgeClient client = new CurseForgeClient(Runnable::run, () -> "");
        String result = client.search("jei", "", "1.21.1", 6, 0, 2, 5, 0).join();
        assertTrue(result.contains("CurseForge API key is not configured"));
        assertTrue(CurseForgeClient.isError(result));
    }
}
