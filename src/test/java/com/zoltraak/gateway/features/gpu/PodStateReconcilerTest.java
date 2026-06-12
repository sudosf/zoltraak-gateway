package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.adapters.gpu.GpuProviderPort;
import com.zoltraak.gateway.adapters.gpu.ProviderException;
import com.zoltraak.gateway.domain.enums.GpuProvider;
import com.zoltraak.gateway.domain.enums.PodStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PodStateReconcilerTest {

    @Mock
    private GpuProviderPort gpuProviderPort;

    @Mock
    private GpuLifecycleManager gpuLifecycleManager;

    private PodStateReconciler reconciler;

    @BeforeEach
    void setUp() {
        reconciler = new PodStateReconciler(gpuProviderPort, gpuLifecycleManager);
    }

    @Nested
    class WhenStatusIsRetrieved {

        @Test
        void forwardsActualStatus_toLifecycleManager() {
            when(gpuProviderPort.getStatus()).thenReturn(Mono.just(PodStatus.STOPPED));

            reconciler.reconcile();

            verify(gpuLifecycleManager).onExternalStateDrift(PodStatus.STOPPED);
        }

        @Test
        void forwardsWarmingStatus_toLifecycleManager() {
            when(gpuProviderPort.getStatus()).thenReturn(Mono.just(PodStatus.WARMING));

            reconciler.reconcile();

            verify(gpuLifecycleManager).onExternalStateDrift(PodStatus.WARMING);
        }
    }

    @Nested
    class WhenStatusRetrievalFails {
        @Test
        void andNoInstancesExist_treatsAsStopped() {
            when(gpuProviderPort.getStatus())
                    .thenReturn(Mono.error(new ProviderException(
                            GpuProvider.VASTAI, 404, "No active GPU instances found on Vast.ai")));

            reconciler.reconcile();

            verify(gpuLifecycleManager).onExternalStateDrift(PodStatus.STOPPED);
        }

        @Test
        void andOtherErrorOccurs_doesNotCallLifecycleManager() {
            when(gpuProviderPort.getStatus())
                    .thenReturn(Mono.error(new ProviderException(GpuProvider.VASTAI, 500, "boom")));

            reconciler.reconcile();

            verify(gpuLifecycleManager, never()).onExternalStateDrift(any());
        }
    }
}