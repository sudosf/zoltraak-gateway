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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GpuLifecycleManagerTest {

    @Mock
    private GpuProviderPort gpuProviderPort;

    @Mock
    private ProviderProperties providerProperties;

    private GpuLifecycleManager gpuLifecycleManager;

    @BeforeEach
    void setUp() {
        when(this.providerProperties.getActive()).thenReturn(GpuProvider.VASTAI);

        gpuLifecycleManager = new GpuLifecycleManager(gpuProviderPort, this.providerProperties);
    }

    @Test
    void podStatus_returnsStopped_whenGpuLifecycleManagerInitialized() {
        assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.STOPPED);
    }

    @Test
    void requestShutdown_whenAlreadyStopped_isNoOpAndDoesNotCallProvider() {
        StepVerifier.create(gpuLifecycleManager.requestShutdown()).verifyComplete();

        verify(gpuProviderPort, never()).stop();
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
        void onProviderFailure_rollsBackToPreviousStatus() {
            when(gpuProviderPort.start()).thenReturn(Mono.error(new ProviderException(
                    GpuProvider.VASTAI, 404, "No active GPU instances found on Vast.ai")));

            StepVerifier.create(gpuLifecycleManager.requestStart())
                    .expectError(ProviderException.class)
                    .verify();

            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.STOPPED);
        }

        @Test
        void whenAlreadyStarting_isNoOpAndDoesNotCallProvider() {
            when(gpuProviderPort.start()).thenReturn(Mono.never());
            gpuLifecycleManager.requestStart().subscribe();

            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(PodStatus.STARTING);

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
    }

    @Nested
    class RequestShutdown {

        @BeforeEach
        void driveToReady() {
            when(gpuProviderPort.start()).thenReturn(Mono.empty());
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
            PodStatus previousStatus = gpuLifecycleManager.getStatus();

            when(gpuProviderPort.stop()).thenReturn(Mono.error(new ProviderException(
                    GpuProvider.VASTAI, 404, "Failed to stop pod")));

            StepVerifier.create(gpuLifecycleManager.requestShutdown())
                    .expectError(ProviderException.class)
                    .verify();

            assertThat(gpuLifecycleManager.getStatus()).isEqualTo(previousStatus);
        }
    }
}