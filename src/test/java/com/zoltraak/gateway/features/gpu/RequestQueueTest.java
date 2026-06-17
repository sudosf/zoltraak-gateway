package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.exception.PodNotReadyException;
import com.zoltraak.gateway.domain.exception.RequestExpiredException;
import com.zoltraak.gateway.features.gpu.model.QueuedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class RequestQueueTest {

    private RequestQueue requestQueue;

    @BeforeEach
    void setUp() {
        requestQueue = new RequestQueue();
    }

    private QueuedRequest makeRequest(Runnable task, Consumer<Throwable> onFailure) {
        return new QueuedRequest(UUID.randomUUID().toString(), LocalDateTime.now(), task, onFailure);
    }

    @Nested
    class OnPodReady {

        @Test
        void runsAllQueuedTasks() {
            AtomicInteger runCount = new AtomicInteger(0);
            requestQueue.enqueue(makeRequest(runCount::incrementAndGet, _ -> {
            }));
            requestQueue.enqueue(makeRequest(runCount::incrementAndGet, _ -> {
            }));

            requestQueue.onPodReady();

            assertThat(runCount.get()).isEqualTo(2);
        }
    }

    @Nested
    class OnPodStartFailed {

        @Test
        void callsOnFailureForAllRequests_withPodNotReadyException() {
            AtomicReference<Throwable> first = new AtomicReference<>();
            AtomicReference<Throwable> second = new AtomicReference<>();
            requestQueue.enqueue(makeRequest(() -> {
            }, first::set));
            requestQueue.enqueue(makeRequest(() -> {
            }, second::set));

            requestQueue.onPodStartFailed(PodStatus.STOPPED);

            assertThat(first.get()).isInstanceOf(PodNotReadyException.class);
            assertThat(second.get()).isInstanceOf(PodNotReadyException.class);
            assertThat(requestQueue.isEmpty()).isTrue();
        }
    }

    @Nested
    class OnPodDegraded {

        @Test
        void callsOnFailureForAllRequests_withPodNotReadyException() {
            AtomicReference<Throwable> captured = new AtomicReference<>();
            requestQueue.enqueue(makeRequest(() -> {
            }, captured::set));

            requestQueue.onPodDegraded();

            assertThat(captured.get()).isInstanceOf(PodNotReadyException.class);
            assertThat(requestQueue.isEmpty()).isTrue();
        }
    }

    @Nested
    class EvictExpiredRequests {

        final int maxWaitMinutes = 5;

        @Test
        void evictsExpiredRequests_andCallsOnFailure_withRequestExpiredException() {
            AtomicReference<Throwable> captured = new AtomicReference<>();
            requestQueue.enqueue(new QueuedRequest(
                    UUID.randomUUID().toString(),
                    LocalDateTime.now().minusMinutes(maxWaitMinutes + 1),
                    () -> {
                    },
                    captured::set
            ));

            requestQueue.evictExpiredRequests(maxWaitMinutes);

            assertThat(captured.get()).isInstanceOf(RequestExpiredException.class);
            assertThat(requestQueue.isEmpty()).isTrue();
        }

        @Test
        void doesNotEvictRequests_thatHaveNotExpired() {
            requestQueue.enqueue(new QueuedRequest(
                    UUID.randomUUID().toString(),
                    LocalDateTime.now(),
                    () -> {
                    },
                    _ -> {
                    }
            ));

            requestQueue.evictExpiredRequests(maxWaitMinutes);

            assertThat(requestQueue.isEmpty()).isFalse();
        }

        @Test
        void requestEnqueuedExactlyAtThreshold_isNotEvicted() {
            requestQueue.enqueue(new QueuedRequest(
                    UUID.randomUUID().toString(),
                    LocalDateTime.now().minusMinutes(maxWaitMinutes),
                    () -> {
                    },
                    _ -> {
                    }
            ));

            requestQueue.evictExpiredRequests(maxWaitMinutes);

            assertThat(requestQueue.isEmpty()).isFalse();
        }

        @Test
        void onlyEvictsExpiredRequests_inMixedQueue() {
            AtomicReference<Throwable> expiredFailure = new AtomicReference<>();
            AtomicBoolean freshFailed = new AtomicBoolean(false);

            requestQueue.enqueue(new QueuedRequest(
                    UUID.randomUUID().toString(),
                    LocalDateTime.now().minusMinutes(maxWaitMinutes + 1),
                    () -> {
                    },
                    expiredFailure::set
            ));
            requestQueue.enqueue(new QueuedRequest(
                    UUID.randomUUID().toString(),
                    LocalDateTime.now(),
                    () -> {
                    },
                    _ -> freshFailed.set(true)
            ));

            requestQueue.evictExpiredRequests(maxWaitMinutes);

            assertThat(expiredFailure.get()).isInstanceOf(RequestExpiredException.class);
            assertThat(freshFailed.get()).isFalse();
            assertThat(requestQueue.isEmpty()).isFalse();
        }
    }
}