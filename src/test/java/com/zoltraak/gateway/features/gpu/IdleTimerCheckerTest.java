package com.zoltraak.gateway.features.gpu;

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

        @ParameterizedTest
        @EnumSource(value = PodStatus.class, names = {"STOPPED", "STOPPING"})
        void skipsCheck_doesNotCheckIdleTimer(PodStatus status) {
            when(gpuLifecycleManager.getStatus()).thenReturn(status);

            idleTimerChecker.check();

            verify(gpuLifecycleManager, never()).requestShutdown();
        }

        @Test
        void andSessionStartedAtIsNull_doesNotCheckIdleTimer() {
            when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.STARTING);
            when(gpuLifecycleManager.getLastActivityAt()).thenReturn(null);

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
        void andBelowIdleThreshold_doesNotRequestShutdown() {
            final int ELAPSED_MINUTES = 14;

            when(gpuLifecycleManager.getLastActivityAt())
                    .thenReturn(LocalDateTime.now().minusMinutes(ELAPSED_MINUTES));

            idleTimerChecker.check();

            verify(gpuLifecycleManager, never()).requestShutdown();
        }

        @Test
        void andIdleThresholdReached_requestsShutdown() {
            when(gpuLifecycleManager.getLastActivityAt())
                    .thenReturn(LocalDateTime.now().minusMinutes(idleTimeoutMinutes));
            when(gpuLifecycleManager.requestShutdown()).thenReturn(Mono.empty());

            idleTimerChecker.check();

            verify(gpuLifecycleManager).requestShutdown();
        }

        @Test
        void andExceedingIdleThreshold_requestsShutdown() {
            final int ELAPSED_MINUTES = 20;

            when(gpuLifecycleManager.getLastActivityAt())
                    .thenReturn(LocalDateTime.now().minusMinutes(ELAPSED_MINUTES));
            when(gpuLifecycleManager.requestShutdown()).thenReturn(Mono.empty());

            idleTimerChecker.check();

            verify(gpuLifecycleManager).requestShutdown();
        }

        @Test
        void andShutdownFails_doesNotPropagateError() {
            when(gpuLifecycleManager.getLastActivityAt())
                    .thenReturn(LocalDateTime.now().minusMinutes(idleTimeoutMinutes));
            when(gpuLifecycleManager.requestShutdown())
                    .thenReturn(Mono.error(new RuntimeException("shutdown failed")));

            assertThatNoException().isThrownBy(() -> idleTimerChecker.check());
        }
    }
}