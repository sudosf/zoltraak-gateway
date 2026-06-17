package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.adapters.gpu.GpuProvider;
import com.zoltraak.gateway.adapters.gpu.ProviderException;
import com.zoltraak.gateway.config.properties.ProviderProperties;
import com.zoltraak.gateway.domain.enums.GpuProviderType;
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
    private GpuProvider gpuProvider;

    @Mock
    private ProviderProperties providerProperties;

    @Mock
    private RequestQueue requestQueue;

    private GpuLifecycleManager gpuLifecycleManager;

    @BeforeEach
    void setUp() {
        when(providerProperties.getActive()).thenReturn(GpuProviderType.VASTAI);
        gpuLifecycleManager = new GpuLifecycleManager(gpuProvider, providerProperties, requestQueue);
    }

    @Test
    void initialStatus_isStopped() {
        assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.STOPPED);
    }

    @Test
    void requestShutdown_whenAlreadyStopped_isNoOpAndDoesNotCallProvider() {
        StepVerifier.create(gpuLifecycleManager.requestShutdown()).verifyComplete();

        verify(gpuProvider, never()).stop();
    }

    @Nested
    class Init {

        @Test
        void onProviderSuccess_syncsStatus() {
            when(gpuProvider.getStatus()).thenReturn(Mono.just(PodStatus.WARMING));

            gpuLifecycleManager.init();

            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.WARMING);
        }

        @Test
        void onProviderSuccess_whenStatusIsNotStopped_setsTimestamps() {
            when(gpuProvider.getStatus()).thenReturn(Mono.just(PodStatus.WARMING));

            gpuLifecycleManager.init();

            assertThat(gpuLifecycleManager.getSessionStartedAt()).isNotNull();
            assertThat(gpuLifecycleManager.getLastActivityAt()).isNotNull();
        }

        @Test
        void onProviderSuccess_whenStatusIsStopped_doesNotSetTimestamps() {
            when(gpuProvider.getStatus()).thenReturn(Mono.just(PodStatus.STOPPED));

            gpuLifecycleManager.init();

            assertThat(gpuLifecycleManager.getSessionStartedAt()).isNull();
            assertThat(gpuLifecycleManager.getLastActivityAt()).isNull();
        }

        @Test
        void onProviderFailure_retainsStoppedStatus_andDoesNotThrow() {
            when(gpuProvider.getStatus()).thenReturn(Mono.error(new ProviderException(
                    GpuProviderType.VASTAI, 500, "Empty response from Vast.ai")));

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

        @Nested
        class ExternalStateDrift {

            @Test
            void whenActualIsStopped_andCurrentIsReady_resetsToStopped() {
                gpuLifecycleManager.onPodReady();

                gpuLifecycleManager.onExternalStateDrift(PodStatus.STOPPED);

                assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.STOPPED);
            }

            @Test
            void whenActualIsStopped_andCurrentIsWarming_resetsToStopped() {
                when(gpuProvider.start()).thenReturn(Mono.empty());
                StepVerifier.create(gpuLifecycleManager.requestStart()).verifyComplete();

                gpuLifecycleManager.onExternalStateDrift(PodStatus.STOPPED);

                assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.STOPPED);
            }

            @Test
            void whenActualIsWarming_andCurrentIsReady_isNoOp() {
                gpuLifecycleManager.onPodReady();

                gpuLifecycleManager.onExternalStateDrift(PodStatus.WARMING);

                assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.READY);
            }
        }
    }

    @Nested
    class RequestStart {

        @Test
        void setsStatusToStarting_beforeProviderResponds() {
            AtomicReference<PodStatus> statusDuringCall = new AtomicReference<>();

            when(gpuProvider.start()).thenAnswer(_ -> {
                statusDuringCall.set(gpuLifecycleManager.getStatus());
                return Mono.empty();
            });

            StepVerifier.create(gpuLifecycleManager.requestStart()).verifyComplete();

            assertThat(statusDuringCall.get()).isEqualTo(PodStatus.STARTING);
        }

        @Test
        void onProviderSuccess_transitionsToWarming() {
            when(gpuProvider.start()).thenReturn(Mono.empty());

            StepVerifier.create(gpuLifecycleManager.requestStart()).verifyComplete();

            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.WARMING);
        }

        @Test
        void onProviderFailure_rollsBackToPreviousStatus_andNotifiesQueue() {
            when(gpuProvider.start()).thenReturn(Mono.error(new ProviderException(
                    GpuProviderType.VASTAI, 404, "No active GPU instances found on Vast.ai")));

            StepVerifier.create(gpuLifecycleManager.requestStart())
                    .expectError(ProviderException.class)
                    .verify();

            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.STOPPED);
            verify(requestQueue).onPodStartFailed(PodStatus.STOPPED);
        }

        @Test
        void whenAlreadyStarting_isNoOpAndDoesNotCallProvider() {
            when(gpuProvider.start()).thenReturn(Mono.never());
            gpuLifecycleManager.requestStart().subscribe();

            StepVerifier.create(gpuLifecycleManager.requestStart()).verifyComplete();

            verify(gpuProvider, times(1)).start();
            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.STARTING);
        }

        @Test
        void whenAlreadyWarming_isNoOpAndDoesNotCallProvider() {
            when(gpuProvider.start()).thenReturn(Mono.empty());
            StepVerifier.create(gpuLifecycleManager.requestStart()).verifyComplete();
            StepVerifier.create(gpuLifecycleManager.requestStart()).verifyComplete();

            verify(gpuProvider, times(1)).start();
            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.WARMING);
        }

        @Test
        void whenAlreadyReady_isNoOpAndDoesNotCallProvider() {
            gpuLifecycleManager.onPodReady();

            StepVerifier.create(gpuLifecycleManager.requestStart()).verifyComplete();

            verify(gpuProvider, never()).start();
            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.READY);
        }

        @Test
        void whenAlreadyDegraded_isNoOpAndDoesNotCallProvider() {
            gpuLifecycleManager.onPodDegraded();

            StepVerifier.create(gpuLifecycleManager.requestStart()).verifyComplete();

            verify(gpuProvider, never()).start();
            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.DEGRADED);
        }
    }

    @Nested
    class RequestShutdown {

        @BeforeEach
        void driveToWarming() {
            when(gpuProvider.start()).thenReturn(Mono.empty());
            when(requestQueue.isEmpty()).thenReturn(true);
            StepVerifier.create(gpuLifecycleManager.requestStart()).verifyComplete();
        }

        @Test
        void setsStatusToStopping_beforeProviderResponds() {
            AtomicReference<PodStatus> statusDuringCall = new AtomicReference<>();

            when(gpuProvider.stop()).thenAnswer(_ -> {
                statusDuringCall.set(gpuLifecycleManager.getStatus());
                return Mono.empty();
            });

            StepVerifier.create(gpuLifecycleManager.requestShutdown()).verifyComplete();

            assertThat(statusDuringCall.get()).isEqualTo(PodStatus.STOPPING);
        }

        @Test
        void whenAlreadyStopping_isNoOpAndDoesNotCallProvider() {
            when(gpuProvider.stop()).thenReturn(Mono.never());
            gpuLifecycleManager.requestShutdown().subscribe();

            StepVerifier.create(gpuLifecycleManager.requestShutdown()).verifyComplete();

            verify(gpuProvider, times(1)).stop();
            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.STOPPING);
        }

        @Test
        void onProviderSuccess_transitionsToStopped() {
            when(gpuProvider.stop()).thenReturn(Mono.empty());

            StepVerifier.create(gpuLifecycleManager.requestShutdown()).verifyComplete();

            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.STOPPED);
        }

        @Test
        void onProviderFailure_rollsBackToPreviousStatus() {
            PodStatus statusBefore = gpuLifecycleManager.getStatus();

            when(gpuProvider.stop()).thenReturn(Mono.error(new ProviderException(
                    GpuProviderType.VASTAI, 404, "Failed to stop pod")));

            StepVerifier.create(gpuLifecycleManager.requestShutdown())
                    .expectError(ProviderException.class)
                    .verify();

            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(statusBefore);
        }

        @Test
        void whenQueueNotEmpty_isNoOpAndDoesNotCallProvider() {
            when(requestQueue.isEmpty()).thenReturn(false);

            StepVerifier.create(gpuLifecycleManager.requestShutdown()).verifyComplete();

            verify(gpuProvider, never()).stop();
        }
    }
}