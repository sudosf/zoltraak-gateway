package com.zoltraak.gateway.features.gpu.process;

import com.zoltraak.gateway.adapters.ollama.OllamaClient;
import com.zoltraak.gateway.config.properties.GpuProperties;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.features.gpu.GpuLifecycleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarmupPollerTest {

    @Mock
    private GpuLifecycleManager gpuLifecycleManager;

    @Mock
    private OllamaClient ollamaClient;

    @Mock
    private GpuProperties gpuProperties;

    private WarmupPoller poller;

    @BeforeEach
    void setUp() {
        poller = new WarmupPoller(gpuLifecycleManager, ollamaClient, gpuProperties);
    }

    @Nested
    class WhenStatus_IsNotEligibleForPolling {

        @ParameterizedTest
        @EnumSource(value = PodStatus.class, names = {"STOPPED", "STOPPING", "READY", "DEGRADED"})
        void skipsAllChecks(PodStatus status) {
            when(gpuLifecycleManager.getStatus()).thenReturn(status);

            poller.poll();

            verifyNoInteractions(ollamaClient);
        }
    }

    @Nested
    class WhenStatus_IsEligibleForPolling {

        final int warmupTimeoutMinutes = 3;

        @ParameterizedTest
        @EnumSource(value = PodStatus.class, names = {"WARMING", "STARTING"})
        void andWarmupTimedOut_setsDegraded(PodStatus status) {
            when(gpuProperties.getWarmupTimeoutMinutes()).thenReturn(warmupTimeoutMinutes);
            when(gpuLifecycleManager.getStatus()).thenReturn(status);
            when(gpuLifecycleManager.getSessionStartedAt())
                    .thenReturn(LocalDateTime.now().minusMinutes(warmupTimeoutMinutes));

            poller.poll();

            verify(gpuLifecycleManager).onPodDegraded();
            verifyNoInteractions(ollamaClient);
        }

        @Test
        void skipsCheck_whenSessionStartedAtIsNull() {
            when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.STARTING);
            when(gpuLifecycleManager.getSessionStartedAt()).thenReturn(null);

            poller.poll();

            verifyNoInteractions(ollamaClient);
        }

        @Nested
        class WithinWarmupTimeout {

            @BeforeEach
            void setUp() {
                when(gpuProperties.getWarmupTimeoutMinutes()).thenReturn(warmupTimeoutMinutes);
                when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.WARMING);
                when(gpuLifecycleManager.getSessionStartedAt())
                        .thenReturn(LocalDateTime.now().minusMinutes(warmupTimeoutMinutes - 1));
            }

            @Test
            void whenHealthy_setsStatusToReady() {
                when(ollamaClient.isHealthy()).thenReturn(Mono.just(true));

                poller.poll();

                verify(gpuLifecycleManager).onPodReady();
            }

            @Test
            void whenNotHealthy_doesNotChangeStatus() {
                when(ollamaClient.isHealthy()).thenReturn(Mono.just(false));

                poller.poll();

                verify(gpuLifecycleManager, never()).onPodReady();
            }
        }
    }
}