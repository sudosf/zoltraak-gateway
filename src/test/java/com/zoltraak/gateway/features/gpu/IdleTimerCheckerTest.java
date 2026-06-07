package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.config.properties.GpuProperties;
import com.zoltraak.gateway.domain.enums.PodStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdleTimerCheckerTest {

    @Mock
    private GpuLifecycleManager gpuLifecycleManager;

    @Mock
    private GpuProperties gpuProperties;

    private IdleTimerChecker idleTimerChecker;

    @BeforeEach
    void setUp() {
        idleTimerChecker = new IdleTimerChecker(gpuLifecycleManager, gpuProperties);
    }

    @Nested
    class WhenPodIsInactive {

        @Test
        void whenStopped_doesNotCheckIdleTimer() {
            when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.STOPPED);

            idleTimerChecker.check();

            verify(gpuLifecycleManager, never()).requestShutdown();
        }

        @Test
        void whenStopping_doesNotCheckIdleTimer() {
            when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.STOPPING);

            idleTimerChecker.check();

            verify(gpuLifecycleManager, never()).requestShutdown();
        }
    }

    @Nested
    class WhenPodIsActive {

        final int idleTimeoutMinutes = 15;

        @BeforeEach
        void setUp() {
            when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.READY);
            when(gpuProperties.getIdleTimeoutMinutes()).thenReturn(idleTimeoutMinutes);
        }

        @Test
        void whenBelowIdleThreshold_doesNotRequestShutdown() {
            final int ELAPSED_MINUTES = 14;

            when(gpuLifecycleManager.getLastActivityAt())
                    .thenReturn(LocalDateTime.now().minusMinutes(ELAPSED_MINUTES));

            idleTimerChecker.check();

            verify(gpuLifecycleManager, never()).requestShutdown();
        }

        @Test
        void whenIdleThresholdReached_requestsShutdown() {
            when(gpuLifecycleManager.getLastActivityAt())
                    .thenReturn(LocalDateTime.now().minusMinutes(idleTimeoutMinutes));
            when(gpuLifecycleManager.requestShutdown()).thenReturn(Mono.empty());

            idleTimerChecker.check();

            verify(gpuLifecycleManager).requestShutdown();
        }

        @Test
        void whenExceedingIdleThreshold_requestsShutdown() {
            final int ELAPSED_MINUTES = 20;

            when(gpuLifecycleManager.getLastActivityAt())
                    .thenReturn(LocalDateTime.now().minusMinutes(ELAPSED_MINUTES));
            when(gpuLifecycleManager.requestShutdown()).thenReturn(Mono.empty());

            idleTimerChecker.check();

            verify(gpuLifecycleManager).requestShutdown();
        }

        @Test
        void whenShutdownFails_doesNotPropagateError() {
            when(gpuLifecycleManager.getLastActivityAt())
                    .thenReturn(LocalDateTime.now().minusMinutes(idleTimeoutMinutes));
            when(gpuLifecycleManager.requestShutdown())
                    .thenReturn(Mono.error(new RuntimeException("shutdown failed")));

            assertThatNoException().isThrownBy(() -> idleTimerChecker.check());
        }
    }
}