package com.izakji.agent.provider;

import com.izakji.agent.api.CancellationToken;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * One streaming provider HTTP call with cancellation, error reporting, and one transient retry.
 *
 * @author Izakji
 */
final class ProviderStreamingHttp {
    private static final int MAX_ERROR_LINES = 40;

    private ProviderStreamingHttp() {}

    static CompletableFuture<Void> execute(
            HttpClient client,
            HttpRequest request,
            CancellationToken cancellationToken,
            Consumer<Stream<String>> consumer,
            UnaryOperator<String> scrubber) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        AtomicReference<Stream<String>> body = new AtomicReference<>();
        AtomicReference<CompletableFuture<HttpResponse<Stream<String>>>> active = new AtomicReference<>();
        attempt(client, request, cancellationToken, consumer, scrubber, result, body, active, 0);
        result.whenComplete((ignored, failure) -> {
            if (result.isCancelled()) {
                Stream<String> lines = body.getAndSet(null);
                if (lines != null) {
                    lines.close();
                }
                CompletableFuture<HttpResponse<Stream<String>>> inFlight = active.get();
                if (inFlight != null) {
                    inFlight.cancel(true);
                }
            }
        });
        return result;
    }

    private static void attempt(
            HttpClient client,
            HttpRequest request,
            CancellationToken cancellationToken,
            Consumer<Stream<String>> consumer,
            UnaryOperator<String> scrubber,
            CompletableFuture<Void> result,
            AtomicReference<Stream<String>> body,
            AtomicReference<CompletableFuture<HttpResponse<Stream<String>>>> active,
            int attempt) {
        if (result.isDone() || cancellationToken.isCancelled()) {
            result.cancel(true);
            return;
        }
        CompletableFuture<HttpResponse<Stream<String>>> response =
                client.sendAsync(request, HttpResponse.BodyHandlers.ofLines());
        active.set(response);
        response.whenComplete((value, failure) -> {
            if (failure != null) {
                result.completeExceptionally(failure);
                return;
            }
            int status = value.statusCode();
            if (attempt == 0 && (status == 429 || status >= 500)) {
                long delay = retryDelay(value);
                value.body().close();
                CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS)
                        .execute(() -> attempt(
                                client, request, cancellationToken, consumer, scrubber, result, body, active, 1));
                return;
            }
            if (status < 200 || status >= 300) {
                String detail = errorBody(value.body());
                value.body().close();
                String message = "Provider returned HTTP " + status;
                if (!detail.isBlank()) {
                    message += ": " + detail;
                }
                result.completeExceptionally(new IllegalStateException(scrubber.apply(message)));
                return;
            }
            try {
                Stream<String> lines = value.body();
                body.set(lines);
                consumer.accept(lines);
                body.set(null);
                result.complete(null);
            } catch (Throwable streamFailure) {
                result.completeExceptionally(streamFailure);
            }
        });
    }

    private static long retryDelay(HttpResponse<?> response) {
        return response.headers()
                .firstValue("Retry-After")
                .flatMap(value -> {
                    try {
                        return java.util.Optional.of(Math.max(0, Long.parseLong(value.strip())) * 1000);
                    } catch (NumberFormatException ignored) {
                        return java.util.Optional.empty();
                    }
                })
                .orElse(500L);
    }

    private static String errorBody(Stream<String> lines) {
        try (lines) {
            String body = lines.limit(MAX_ERROR_LINES).reduce("", (left, right) -> left + right + "\n");
            return body.length() <= 4096
                    ? body.strip()
                    : body.substring(0, 4096).strip();
        } catch (RuntimeException ignored) {
            return "";
        }
    }
}
