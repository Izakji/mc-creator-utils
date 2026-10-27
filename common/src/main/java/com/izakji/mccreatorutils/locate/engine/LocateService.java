package com.izakji.mccreatorutils.locate.engine;

import com.izakji.mccreatorutils.McCreatorUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Bounded asynchronous locate request service.
 *
 * @author Izakji
 */
public final class LocateService implements AutoCloseable {
    private static final int MAX_CACHE_ENTRIES = 256;

    private volatile int biomeThreads;
    private volatile int maxQueuedSearches;
    private volatile Duration resultCacheTtl;
    private final LongSupplier clockMillis;
    private final ThreadPoolExecutor biomeExecutor;
    private final ThreadPoolExecutor structureExecutor;
    private final Object lock = new Object();
    private final Map<LocateRequestKey, SharedRequest> active = new HashMap<>();
    private final Map<LocateRequestKey, CacheEntry> cache = new HashMap<>();

    private int reservedBiome;
    private int reservedStructure;
    private int queued;
    private boolean closed;

    public LocateService(int biomeThreads, int maxQueuedSearches, Duration resultCacheTtl, LongSupplier clockMillis) {
        this.biomeThreads = Math.max(1, biomeThreads);
        this.maxQueuedSearches = Math.max(0, maxQueuedSearches);
        this.resultCacheTtl = Objects.requireNonNull(resultCacheTtl, "resultCacheTtl");
        this.clockMillis = Objects.requireNonNull(clockMillis, "clockMillis");
        this.biomeExecutor = new ThreadPoolExecutor(
                this.biomeThreads,
                this.biomeThreads,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory("mccreatorutils-locate-biome-"),
                new ThreadPoolExecutor.AbortPolicy());
        this.structureExecutor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory("mccreatorutils-locate-structure-"),
                new ThreadPoolExecutor.AbortPolicy());
    }

    public CompletableFuture<LocateResult> submit(LocateRequestKey key, LocateTask task, long timeoutMillis) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(task, "task");

        synchronized (lock) {
            if (closed) {
                return CompletableFuture.completedFuture(LocateResult.failed(LocateFailure.CANCELLED, 0));
            }

            long now = clockMillis.getAsLong();
            sweepCacheLocked(now);
            CacheEntry cached = cache.get(key);
            if (cached != null) {
                if (cached.expiresAt() > now) {
                    return CompletableFuture.completedFuture(cached.result());
                }
                cache.remove(key);
            }

            SharedRequest existing = active.get(key);
            if (existing != null) {
                existing.callers++;
                return callerFuture(existing);
            }

            Admission admission = admit(key.type());
            if (admission == null) {
                return CompletableFuture.completedFuture(LocateResult.failed(LocateFailure.QUEUE_FULL, 0));
            }

            SharedRequest request =
                    new SharedRequest(key, task, admission.queued(), now, now + Math.max(0L, timeoutMillis));
            active.put(key, request);
            CompletableFuture<LocateResult> caller = callerFuture(request);
            try {
                request.executorFuture = executorFor(key.type()).submit(() -> run(request));
            } catch (RejectedExecutionException exception) {
                active.remove(key, request);
                finish(request, LocateResult.failed(LocateFailure.CANCELLED, 0));
                return CompletableFuture.completedFuture(LocateResult.failed(LocateFailure.CANCELLED, 0));
            }
            return caller;
        }
    }

    public void tick(long nowMillis) {
        List<SharedRequest> expired = new ArrayList<>();
        synchronized (lock) {
            for (SharedRequest request : active.values()) {
                if (!request.completed && nowMillis >= request.deadline) {
                    expired.add(request);
                }
            }
        }
        for (SharedRequest request : expired) {
            cancel(request, LocateFailure.TIMEOUT);
        }
    }

    public void clearCache() {
        synchronized (lock) {
            cache.clear();
        }
    }

    public void cancelAll() {
        List<SharedRequest> requests;
        synchronized (lock) {
            requests = new ArrayList<>(active.values());
        }
        for (SharedRequest request : requests) {
            cancel(request, LocateFailure.CANCELLED);
        }
    }

    public int cancelDimension(ResourceKey<Level> dimension) {
        List<SharedRequest> requests = new ArrayList<>();
        synchronized (lock) {
            for (SharedRequest request : active.values()) {
                if (request.key.dimension().equals(dimension)) {
                    requests.add(request);
                }
            }
        }
        for (SharedRequest request : requests) {
            cancel(request, LocateFailure.CANCELLED);
        }
        return requests.size();
    }

    public int activeCount() {
        synchronized (lock) {
            return active.size();
        }
    }

    public int queuedCount() {
        synchronized (lock) {
            return queued;
        }
    }

    public void reconfigure(int biomeThreads, int maxQueuedSearches, Duration resultCacheTtl) {
        Objects.requireNonNull(resultCacheTtl, "resultCacheTtl");
        int threads;
        synchronized (lock) {
            if (closed) {
                return;
            }
            threads = Math.max(1, biomeThreads);
            this.biomeThreads = threads;
            this.maxQueuedSearches = Math.max(0, maxQueuedSearches);
            this.resultCacheTtl = resultCacheTtl;
        }
        biomeExecutor.setMaximumPoolSize(Math.max(biomeExecutor.getMaximumPoolSize(), threads));
        biomeExecutor.setCorePoolSize(threads);
        biomeExecutor.setMaximumPoolSize(threads);
    }

    int cacheSize() {
        synchronized (lock) {
            sweepCacheLocked(clockMillis.getAsLong());
            return cache.size();
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            cache.clear();
        }

        cancelAll();
        biomeExecutor.shutdownNow();
        structureExecutor.shutdownNow();
        try {
            biomeExecutor.awaitTermination(2, TimeUnit.SECONDS);
            structureExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void run(SharedRequest request) {
        synchronized (lock) {
            if (request.completed) {
                return;
            }
            request.started = true;
            if (request.queued) {
                queued--;
            }
        }

        LocateResult result;
        try {
            request.token.throwIfCancelled();
            result = Objects.requireNonNull(request.task.run(request.token), "locate result");
        } catch (CancellationException exception) {
            result = LocateResult.failed(LocateFailure.CANCELLED, elapsed(request));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            result = LocateResult.failed(
                    request.cancelled.get() ? LocateFailure.CANCELLED : LocateFailure.ERROR, elapsed(request));
        } catch (Throwable throwable) {
            if (request.cancelled.get()) {
                result = LocateResult.failed(LocateFailure.CANCELLED, elapsed(request));
            } else {
                McCreatorUtils.LOGGER.error("Locate task failed for {}", request.key, throwable);
                result = LocateResult.failed(LocateFailure.ERROR, elapsed(request));
            }
        }
        finish(request, result);
    }

    private void cancel(SharedRequest request, LocateFailure failure) {
        LocateResult result = LocateResult.failed(failure, elapsed(request));
        boolean completed;
        synchronized (lock) {
            if (request.completed) {
                return;
            }
            request.cancelled.set(true);
            completed = finishLocked(request, result);
        }
        if (completed) {
            Future<?> executorFuture = request.executorFuture;
            if (executorFuture != null) {
                executorFuture.cancel(true);
            }
            request.future.complete(result);
        }
    }

    private void finish(SharedRequest request, LocateResult result) {
        boolean completed;
        synchronized (lock) {
            completed = finishLocked(request, result);
        }
        if (completed) {
            request.future.complete(result);
        }
    }

    private boolean finishLocked(SharedRequest request, LocateResult result) {
        if (request.completed) {
            return false;
        }
        request.completed = true;
        if (request.queued && !request.started) {
            queued--;
        }
        if (request.key.type() == LocateType.BIOME) {
            reservedBiome--;
        } else {
            reservedStructure--;
        }
        active.remove(request.key, request);

        if (result.success() || result.failure() == LocateFailure.NOT_FOUND) {
            long ttl = resultCacheTtl.toMillis();
            if (ttl > 0) {
                long now = clockMillis.getAsLong();
                cache.put(request.key, new CacheEntry(result, now + ttl));
                sweepCacheLocked(now);
            }
        }
        return true;
    }

    private void sweepCacheLocked(long now) {
        cache.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        while (cache.size() > MAX_CACHE_ENTRIES) {
            Map.Entry<LocateRequestKey, CacheEntry> oldest = null;
            for (Map.Entry<LocateRequestKey, CacheEntry> entry : cache.entrySet()) {
                if (oldest == null
                        || entry.getValue().expiresAt() < oldest.getValue().expiresAt()) {
                    oldest = entry;
                }
            }
            if (oldest == null) {
                return;
            }
            cache.remove(oldest.getKey());
        }
    }

    private Admission admit(LocateType type) {
        if (type == LocateType.BIOME) {
            if (reservedBiome < biomeThreads) {
                reservedBiome++;
                return new Admission(false);
            }
            if (queued < maxQueuedSearches) {
                reservedBiome++;
                queued++;
                return new Admission(true);
            }
            return null;
        }

        if (reservedStructure < 1) {
            reservedStructure++;
            return new Admission(false);
        }
        if (queued < maxQueuedSearches) {
            reservedStructure++;
            queued++;
            return new Admission(true);
        }
        return null;
    }

    private ThreadPoolExecutor executorFor(LocateType type) {
        return type == LocateType.BIOME ? biomeExecutor : structureExecutor;
    }

    private CompletableFuture<LocateResult> callerFuture(SharedRequest request) {
        CompletableFuture<LocateResult> caller = new CompletableFuture<>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean cancelled = super.cancel(mayInterruptIfRunning);
                if (cancelled) {
                    callerCancelled(request);
                }
                return cancelled;
            }
        };
        request.future.whenComplete((result, throwable) -> {
            if (throwable == null) {
                caller.complete(result);
            } else {
                caller.completeExceptionally(throwable);
            }
        });
        return caller;
    }

    private void callerCancelled(SharedRequest request) {
        boolean cancelShared = false;
        synchronized (lock) {
            request.cancelledCallers++;
            if (request.cancelledCallers >= request.callers) {
                cancelShared = true;
            }
        }
        if (cancelShared) {
            cancel(request, LocateFailure.CANCELLED);
        }
    }

    private long elapsed(SharedRequest request) {
        return Math.max(0L, clockMillis.getAsLong() - request.startMillis);
    }

    private static ThreadFactory threadFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private record Admission(boolean queued) {}

    private record CacheEntry(LocateResult result, long expiresAt) {}

    private static final class SharedRequest {
        private final LocateRequestKey key;
        private final LocateTask task;
        private final boolean queued;
        private final long startMillis;
        private final long deadline;
        private final CompletableFuture<LocateResult> future = new CompletableFuture<>();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final CancellationToken token = cancelled::get;

        private volatile boolean started;
        private volatile boolean completed;
        private volatile Future<?> executorFuture;
        private int callers = 1;
        private int cancelledCallers;

        private SharedRequest(LocateRequestKey key, LocateTask task, boolean queued, long startMillis, long deadline) {
            this.key = key;
            this.task = task;
            this.queued = queued;
            this.startMillis = startMillis;
            this.deadline = deadline;
        }
    }
}
