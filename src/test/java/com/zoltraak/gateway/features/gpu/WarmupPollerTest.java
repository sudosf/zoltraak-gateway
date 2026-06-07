package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.adapters.ollama.OllamaPort;
import com.zoltraak.gateway.config.properties.GpuProperties;
import com.zoltraak.gateway.domain.enums.PodStatus;
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
    private OllamaPort ollamaPort;

    @Mock
    private GpuProperties gpuProperties;

    private WarmupPoller poller;

    @BeforeEach
    void setUp() {
        poller = new WarmupPoller(gpuLifecycleManager, ollamaPort, gpuProperties);
    }

    @Nested
    class WhenStatus_IsNotEligibleForPolling {

        @ParameterizedTest
        @EnumSource(value = PodStatus.class, names = {"STOPPED", "STOPPING", "READY", "DEGRADED"})
        void skipsAllChecks(PodStatus status) {
            when(gpuLifecycleManager.getStatus()).thenReturn(status);

            poller.poll();

            verifyNoInteractions(ollamaPort);
            verify(gpuLifecycleManager, never()).setStatus(any());
        }
    }

    @Nested
    class WhenStatus_IsEligibleForPolling {

        final int warmupTimeoutMinutes = 3;

        @BeforeEach
        void setUp() {
            when(gpuProperties.getWarmupTimeoutMinutes()).thenReturn(warmupTimeoutMinutes);
        }

        @ParameterizedTest
        @EnumSource(value = PodStatus.class, names = {"WARMING", "STARTING"})
        void whenWarmupTimedOut_setsDegraded(PodStatus status) {
            when(gpuLifecycleManager.getStatus()).thenReturn(status);
            when(gpuLifecycleManager.getSessionStartedAt())
                    .thenReturn(LocalDateTime.now().minusMinutes(warmupTimeoutMinutes));

            poller.poll();

            verify(gpuLifecycleManager).setStatus(PodStatus.DEGRADED);
            verifyNoInteractions(ollamaPort);
        }

        @Nested
        class WithinWarmupTimeout {

            @BeforeEach
            void setUp() {
                when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.WARMING);
                when(gpuLifecycleManager.getSessionStartedAt())
                        .thenReturn(LocalDateTime.now().minusMinutes(warmupTimeoutMinutes - 1));
            }

            @Test
            void whenHealthy_setsStatusToReady() {
                when(ollamaPort.isHealthy()).thenReturn(Mono.just(true));

                poller.poll();

                verify(gpuLifecycleManager).setStatus(PodStatus.READY);
            }

            @Test
            void whenNotHealthy_doesNotChangeStatus() {
                when(ollamaPort.isHealthy()).thenReturn(Mono.just(false));

                poller.poll();

                verify(gpuLifecycleManager, never()).setStatus(any());
            }
        }
    }
}