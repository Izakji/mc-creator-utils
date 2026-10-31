package com.izakji.mccreatorutils.client.agent.context;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

/**
 * One-hertz ping and Minecraft connection traffic history.
 *
 * @author Izakji
 */
public final class NetworkDiagnostics {
    private static final int CAPACITY = 300;
    private static final long SAMPLE_INTERVAL_NANOS = 1_000_000_000L;
    private static final String HANDLER_NAME = "creator_utils_agent_traffic";
    private static final AtomicLong RECEIVED_BYTES = new AtomicLong();
    private static final AtomicLong SENT_BYTES = new AtomicLong();
    private static final NetworkDiagnostics INSTANCE = new NetworkDiagnostics();

    private final Sample[] samples = new Sample[CAPACITY];
    private int next;
    private int count;
    private long lastSample;
    private long lastCounterNanos;
    private long lastReceived;
    private long lastSent;
    private Channel attachedChannel;

    private NetworkDiagnostics() {}

    public static NetworkDiagnostics instance() {
        return INSTANCE;
    }

    public void sampleIfDue(Minecraft minecraft) {
        long now = System.nanoTime();
        synchronized (this) {
            if (lastSample != 0 && now - lastSample < SAMPLE_INTERVAL_NANOS) {
                return;
            }
            lastSample = now;
            try {
                samples[next] = sample(minecraft, now);
            } catch (RuntimeException failure) {
                samples[next] = new Sample(System.currentTimeMillis(), -1, 0, 0);
            }
            next = (next + 1) % CAPACITY;
            count = Math.min(CAPACITY, count + 1);
        }
    }

    public synchronized Snapshot snapshot() {
        Sample[] ordered = new Sample[count];
        int start = (next - count + CAPACITY) % CAPACITY;
        for (int index = 0; index < count; index++) {
            ordered[index] = samples[(start + index) % CAPACITY];
        }
        return new Snapshot(ordered);
    }

    private Sample sample(Minecraft minecraft, long now) {
        ClientPacketListener listener = minecraft.getConnection();
        attach(listener);
        int ping = ping(listener, minecraft);
        long received = RECEIVED_BYTES.get();
        long sent = SENT_BYTES.get();
        long elapsed = lastCounterNanos == 0 ? 0 : now - lastCounterNanos;
        long rxRate = 0;
        long txRate = 0;
        if (!isLocalConnection(listener)) {
            if (elapsed > 0 && attachedChannel != null) {
                rxRate = Math.max(0, (received - lastReceived) * 1_000_000_000L / elapsed);
                txRate = Math.max(0, (sent - lastSent) * 1_000_000_000L / elapsed);
            }
            lastReceived = received;
            lastSent = sent;
            lastCounterNanos = now;
        }
        return new Sample(System.currentTimeMillis(), ping, rxRate, txRate);
    }

    private void attach(ClientPacketListener listener) {
        if (listener == null) {
            attachedChannel = null;
            lastCounterNanos = 0;
            return;
        }
        Channel channel = listener.getConnection().channel();
        if (channel == attachedChannel) {
            return;
        }
        attachedChannel = channel;
        lastCounterNanos = 0;
        try {
            if (channel.pipeline().get(HANDLER_NAME) == null) {
                channel.pipeline().addFirst(HANDLER_NAME, new TrafficHandler());
            }
        } catch (RuntimeException ignored) {
            // Traffic counters are optional; ping stats still work.
        }
    }

    private static boolean isLocalConnection(ClientPacketListener listener) {
        return listener == null || listener.getConnection().isMemoryConnection();
    }

    private static int ping(ClientPacketListener listener, Minecraft minecraft) {
        if (listener == null || minecraft.player == null) {
            return -1;
        }
        PlayerInfo info = listener.getPlayerInfo(minecraft.player.getUUID());
        return info == null ? -1 : info.getLatency();
    }

    /**
     * One network sample.
     *
     * @param epochMillis sample time
     * @param pingMs ping in milliseconds, or -1
     * @param receivedBytesPerSecond receive rate
     * @param sentBytesPerSecond send rate
     */
    public record Sample(long epochMillis, int pingMs, long receivedBytesPerSecond, long sentBytesPerSecond) {}

    /**
     * Immutable network history in chronological order.
     *
     * @param samples samples
     */
    public record Snapshot(Sample[] samples) {
        public int size() {
            return samples.length;
        }

        public static Snapshot empty() {
            return new Snapshot(new Sample[0]);
        }
    }

    private static final class TrafficHandler extends ChannelDuplexHandler {
        @Override
        public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
            if (message instanceof ByteBuf buffer) {
                RECEIVED_BYTES.addAndGet(buffer.readableBytes());
            }
            super.channelRead(context, message);
        }

        @Override
        public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
            if (message instanceof ByteBuf buffer) {
                SENT_BYTES.addAndGet(buffer.readableBytes());
            }
            super.write(context, message, promise);
        }
    }
}
