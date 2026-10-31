package com.izakji.mccreatorutils.client.agent;

/**
 * Frame selection for the small walking-pig response loader.
 *
 * @author Izakji
 */
final class AgentPigAnimation {
    private static final int FRAMES = 4;
    private static final long FRAME_MILLIS = 160;

    private AgentPigAnimation() {}

    static int frame(long millis) {
        return (int) ((millis / FRAME_MILLIS) % FRAMES);
    }

    static int frameCount() {
        return FRAMES;
    }
}
