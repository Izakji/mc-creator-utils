package com.izakji.mccreatorutils.locate.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(5)
class LocateServiceTest {

    @Test
    void deduplicatesIdenticalRequests() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var runs = new AtomicInteger();
        try (var service = service(1, 8)) {
            LocateTask task = token -> {
                runs.incrementAndGet();
                started.countDown();
                release.await();
                return LocateResult.found(BlockPos.ZERO, Holder.direct(null), 0);
            };
            var key = key("minecraft:plains");
            var first = service.submit(key, task, 5_000);
            assertTrue(started.await(1, TimeUnit.SECONDS));
            var second = service.submit(key, task, 5_000);
            release.countDown();

            assertEquals(
                    first.get(1, TimeUnit.SECONDS).position(),
                    second.get(1, TimeUnit.SECONDS).position());
            assertEquals(1, runs.get());
        }
    }

    @Test
    void rejectsWhenQueueIsFull() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var service = service(1, 0)) {
            var first = service.submit(
                    key("minecraft:plains"),
                    token -> {
                        started.countDown();
                        release.await();
                        return LocateResult.notFound(0);
                    },
                    5_000);
            assertTrue(started.await(1, TimeUnit.SECONDS));

            var second = service.submit(key("minecraft:forest"), token -> LocateResult.notFound(0), 5_000);
            assertEquals(LocateFailure.QUEUE_FULL, second.join().failure());

            release.countDown();
            first.get(1, TimeUnit.SECONDS);
        }
    }

    @Test
    void cachesOnlyCompletedResultsUntilExpiry() throws Exception {
        var clock = new AtomicLong(1_000);
        var runs = new AtomicInteger();
        try (var service = new LocateService(1, 2, Duration.ofSeconds(30), clock::get)) {
            var key = key("minecraft:plains");
            var result = service.submit(
                            key,
                            token -> {
                                runs.incrementAndGet();
                                return LocateResult.found(BlockPos.ZERO, Holder.direct(null), 25);
                            },
                            1_000)
                    .get(1, TimeUnit.SECONDS);
            assertEquals(25, result.elapsedMillis());

            service.submit(
                            key,
                            token -> {
                                runs.incrementAndGet();
                                return LocateResult.notFound(0);
                            },
                            1_000)
                    .get(1, TimeUnit.SECONDS);
            assertEquals(1, runs.get());

            clock.addAndGet(31_000);
            service.submit(
                            key,
                            token -> {
                                runs.incrementAndGet();
                                return LocateResult.notFound(0);
                            },
                            1_000)
                    .get(1, TimeUnit.SECONDS);
            assertEquals(2, runs.get());
        }
    }

    @Test
    void timesOutRunningRequest() throws Exception {
        var clock = new AtomicLong(1_000);
        var started = new CountDownLatch(1);
        try (var service = new LocateService(1, 2, Duration.ofSeconds(30), clock::get)) {
            var future = service.submit(
                    key("minecraft:plains"),
                    token -> {
                        started.countDown();
                        while (!token.isCancelled()) {
                            Thread.sleep(5);
                        }
                        return LocateResult.failed(LocateFailure.CANCELLED, 0);
                    },
                    100);
            assertTrue(started.await(1, TimeUnit.SECONDS));
            service.tick(clock.get() + 101);
            assertEquals(LocateFailure.TIMEOUT, future.get(1, TimeUnit.SECONDS).failure());
        }
    }

    @Test
    void cancelAllCompletesActiveRequestsAsCancelled() throws Exception {
        var started = new CountDownLatch(1);
        try (var service = service(1, 2)) {
            var future = service.submit(
                    key("minecraft:plains"),
                    token -> {
                        started.countDown();
                        while (!token.isCancelled()) {
                            Thread.sleep(5);
                        }
                        return LocateResult.failed(LocateFailure.CANCELLED, 0);
                    },
                    5_000);
            assertTrue(started.await(1, TimeUnit.SECONDS));
            service.cancelAll();
            assertEquals(
                    LocateFailure.CANCELLED, future.get(1, TimeUnit.SECONDS).failure());
        }
    }

    private static LocateService service(int biomeThreads, int queued) {
        return new LocateService(biomeThreads, queued, Duration.ofSeconds(30), System::currentTimeMillis);
    }

    private static LocateRequestKey key(String target) {
        return new LocateRequestKey(
                Level.OVERWORLD, LocateType.BIOME, List.of(ResourceLocation.parse(target)), BlockPos.ZERO, 6400, false);
    }
}
