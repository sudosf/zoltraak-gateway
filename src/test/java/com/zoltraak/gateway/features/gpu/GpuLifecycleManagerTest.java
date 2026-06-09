package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.adapters.gpu.GpuProviderPort;
import com.zoltraak.gateway.adapters.gpu.ProviderException;
import com.zoltraak.gateway.config.properties.ProviderProperties;
import com.zoltraak.gateway.domain.enums.GpuProvider;
import com.zoltraak.gateway.domain.enums.PodStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GpuLifecycleManagerTest {

    @Mock
    private GpuProviderPort gpuProviderPort;

    @Mock
    private ProviderProperties providerProperties;

    @Mock
    private RequestQueue requestQueue;

    private GpuLifecycleManager gpuLifecycleManager;

    @BeforeEach
    void setUp() {
        when(providerProperties.getActive()).thenReturn(GpuProvider.VASTAI);
        gpuLifecycleManager = new GpuLifecycleManager(gpuProviderPort, providerProperties, requestQueue);
    }

    @Test
    void initialStatus_isStopped() {
        assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.STOPPED);
    }

    @Test
    void requestShutdown_whenAlreadyStopped_isNoOpAndDoesNotCallProvider() {
        StepVerifier.create(gpuLifecycleManager.requestShutdown()).verifyComplete();

        verify(gpuProviderPort, never()).stop();
    }

    @Nested
    class Init {

        @Test
        void onProviderSuccess_syncsStatus() {
            when(gpuProviderPort.getStatus()).thenReturn(Mono.just(PodStatus.WARMING));

            gpuLifecycleManager.init();

            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.WARMING);
        }

        @Test
        void onProviderSuccess_whenStatusIsNotStopped_setsTimestamps() {
            when(gpuProviderPort.getStatus()).thenReturn(Mono.just(PodStatus.WARMING));

            gpuLifecycleManager.init();

            assertThat(gpuLifecycleManager.getSessionStartedAt()).isNotNull();
            assertThat(gpuLifecycleManager.getLastActivityAt()).isNotNull();
        }

        @Test
        void onProviderSuccess_whenStatusIsStopped_doesNotSetTimestamps() {
            when(gpuProviderPort.getStatus()).thenReturn(Mono.just(PodStatus.STOPPED));

            gpuLifecycleManager.init();

            assertThat(gpuLifecycleManager.getSessionStartedAt()).isNull();
            assertThat(gpuLifecycleManager.getLastActivityAt()).isNull();
        }

        @Test
        void onProviderFailure_retainsStoppedStatus_andDoesNotThrow() {
            when(gpuProviderPort.getStatus()).thenReturn(Mono.error(new ProviderException(
                    GpuProvider.VASTAI, 500, "Empty response from Vast.ai")));

            assertThatNoException().isThrownBy(() -> gpuLifecycleManager.init());
            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.STOPPED);
        }
    }

    @Nested
    class PodEvents {

        @Test
        void onPodReady_setsStatusToReady_andNotifiesQueue() {
            gpuLifecycleManager.onPodReady();

            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.READY);
            verify(requestQueue).onPodReady();
        }

        @Test
        void onPodDegraded_setsStatusToDegraded_andNotifiesQueue() {
            gpuLifecycleManager.onPodDegraded();

            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.DEGRADED);
            verify(requestQueue).onPodDegraded();
        }
    }

    @Nested
    class RequestStart {

        @Test
        void setsStatusToStarting_beforeProviderResponds() {
            AtomicReference<PodStatus> statusDuringCall = new AtomicReference<>();

            when(gpuProviderPort.start()).thenAnswer(_ -> {
                statusDuringCall.set(gpuLifecycleManager.getStatus());
                return Mono.empty();
            });

            StepVerifier.create(gpuLifecycleManager.requestStart()).verifyComplete();

            assertThat(statusDuringCall.get()).isEqualTo(PodStatus.STARTING);
        }

        @Test
        void onProviderSuccess_transitionsToWarming() {
            when(gpuProviderPort.start()).thenReturn(Mono.empty());

            StepVerifier.create(gpuLifecycleManager.requestStart()).verifyComplete();

            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.WARMING);
        }

        @Test
        void onProviderFailure_rollsBackToPreviousStatus_andNotifiesQueue() {
            when(gpuProviderPort.start()).thenReturn(Mono.error(new ProviderException(
                    GpuProvider.VASTAI, 404, "No active GPU instances found on Vast.ai")));

            StepVerifier.create(gpuLifecycleManager.requestStart())
                    .expectError(ProviderException.class)
                    .verify();

            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.STOPPED);
            verify(requestQueue).onPodStartFailed(PodStatus.STOPPED);
        }

        @Test
        void whenAlreadyStarting_isNoOpAndDoesNotCallProvider() {
            when(gpuProviderPort.start()).thenReturn(Mono.never());
            gpuLifecycleManager.requestStart().subscribe();

            StepVerifier.create(gpuLifecycleManager.requestStart()).verifyComplete();

            verify(gpuProviderPort, times(1)).start();
            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.STARTING);
        }

        @Test
        void whenAlreadyWarming_isNoOpAndDoesNotCallProvider() {
            when(gpuProviderPort.start()).thenReturn(Mono.empty());
            StepVerifier.create(gpuLifecycleManager.requestStart()).verifyComplete();
            StepVerifier.create(gpuLifecycleManager.requestStart()).verifyComplete();

            verify(gpuProviderPort, times(1)).start();
            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.WARMING);
        }

        @Test
        void whenAlreadyReady_isNoOpAndDoesNotCallProvider() {
            gpuLifecycleManager.onPodReady();

            StepVerifier.create(gpuLifecycleManager.requestStart()).verifyComplete();

            verify(gpuProviderPort, never()).start();
            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.READY);
        }

        @Test
        void whenAlreadyDegraded_isNoOpAndDoesNotCallProvider() {
            gpuLifecycleManager.onPodDegraded();

            StepVerifier.create(gpuLifecycleManager.requestStart()).verifyComplete();

            verify(gpuProviderPort, never()).start();
            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.DEGRADED);
        }
    }

    @Nested
    class RequestShutdown {

        @BeforeEach
        void driveToWarming() {
            when(gpuProviderPort.start()).thenReturn(Mono.empty());
            when(requestQueue.isEmpty()).thenReturn(true);
            StepVerifier.create(gpuLifecycleManager.requestStart()).verifyComplete();
        }

        @Test
        void setsStatusToStopping_beforeProviderResponds() {
            AtomicReference<PodStatus> statusDuringCall = new AtomicReference<>();

            when(gpuProviderPort.stop()).thenAnswer(_ -> {
                statusDuringCall.set(gpuLifecycleManager.getStatus());
                return Mono.empty();
            });

            StepVerifier.create(gpuLifecycleManager.requestShutdown()).verifyComplete();

            assertThat(statusDuringCall.get()).isEqualTo(PodStatus.STOPPING);
        }

        @Test
        void whenAlreadyStopping_isNoOpAndDoesNotCallProvider() {
            when(gpuProviderPort.stop()).thenReturn(Mono.never());
            gpuLifecycleManager.requestShutdown().subscribe();

            StepVerifier.create(gpuLifecycleManager.requestShutdown()).verifyComplete();

            verify(gpuProviderPort, times(1)).stop();
            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.STOPPING);
        }

        @Test
        void onProviderSuccess_transitionsToStopped() {
            when(gpuProviderPort.stop()).thenReturn(Mono.empty());

            StepVerifier.create(gpuLifecycleManager.requestShutdown()).verifyComplete();

            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.STOPPED);
        }

        @Test
        void onProviderFailure_rollsBackToPreviousStatus() {
            PodStatus statusBefore = gpuLifecycleManager.getStatus();

            when(gpuProviderPort.stop()).thenReturn(Mono.error(new ProviderException(
                    GpuProvider.VASTAI, 404, "Failed to stop pod")));

            StepVerifier.create(gpuLifecycleManager.requestShutdown())
                    .expectError(ProviderException.class)
                    .verify();

            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(statusBefore);
        }

        @Test
        void whenQueueNotEmpty_isNoOpAndDoesNotCallProvider() {
            when(requestQueue.isEmpty()).thenReturn(false);

            StepVerifier.create(gpuLifecycleManager.requestShutdown()).verifyComplete();

            verify(gpuProviderPort, never()).stop();
        }
    }
}