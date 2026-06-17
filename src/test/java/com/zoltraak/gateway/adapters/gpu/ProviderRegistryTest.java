package com.zoltraak.gateway.adapters.gpu;

import com.zoltraak.gateway.adapters.gpu.runpod.RunpodAdapter;
import com.zoltraak.gateway.adapters.gpu.vastai.VastAiAdapter;
import com.zoltraak.gateway.config.properties.ProviderProperties;
import com.zoltraak.gateway.domain.enums.GpuProviderType;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.models.provider.PodConnectionDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderRegistryTest {

    @Mock
    private VastAiAdapter vastAiAdapter;

    @Mock
    private RunpodAdapter runpodAdapter;

    @Mock
    private ProviderProperties providerProperties;

    private ProviderRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ProviderRegistry(providerProperties, vastAiAdapter, runpodAdapter);
    }

    @Nested
    class WhenProviderIsVastAi {

        @BeforeEach
        void setUp() {
            when(providerProperties.getActive()).thenReturn(GpuProviderType.VASTAI);
        }

        @Test
        void delegatesStart_toVastAiAdapter() {
            when(vastAiAdapter.start()).thenReturn(Mono.empty());

            StepVerifier.create(registry.start())
                    .verifyComplete();

            verify(vastAiAdapter).start();
        }

        @Test
        void delegatesStop_toVastAiAdapter() {
            when(vastAiAdapter.stop()).thenReturn(Mono.empty());

            StepVerifier.create(registry.stop())
                    .verifyComplete();

            verify(vastAiAdapter).stop();
        }

        @Test
        void delegatesGetStatus_toVastAiAdapter() {
            when(vastAiAdapter.getStatus()).thenReturn(Mono.just(PodStatus.READY));

            StepVerifier.create(registry.getStatus())
                    .expectNext(PodStatus.READY)
                    .verifyComplete();

            verify(vastAiAdapter).getStatus();
        }

        @Test
        void delegatesGetConnectionDetails_toVastAiAdapter() {
            PodConnectionDetails details = new PodConnectionDetails("http://1.2.3.4:11434", null);
            when(vastAiAdapter.getConnectionDetails()).thenReturn(Mono.just(details));

            StepVerifier.create(registry.getConnectionDetails())
                    .expectNext(details)
                    .verifyComplete();

            verify(vastAiAdapter).getConnectionDetails();
        }
    }

    @Nested
    class WhenProviderIsRunPod {

        @BeforeEach
        void setUp() {
            when(providerProperties.getActive()).thenReturn(GpuProviderType.RUNPOD);
        }

        @Test
        void delegatesStart_toRunpodAdapter() {
            when(runpodAdapter.start()).thenReturn(Mono.empty());

            StepVerifier.create(registry.start())
                    .verifyComplete();

            verify(runpodAdapter).start();
        }

        @Test
        void delegatesStop_toRunpodAdapter() {
            when(runpodAdapter.stop()).thenReturn(Mono.empty());

            StepVerifier.create(registry.stop())
                    .verifyComplete();

            verify(runpodAdapter).stop();
        }

        @Test
        void delegatesGetStatus_toRunpodAdapter() {
            when(runpodAdapter.getStatus()).thenReturn(Mono.just(PodStatus.READY));

            StepVerifier.create(registry.getStatus())
                    .expectNext(PodStatus.READY)
                    .verifyComplete();

            verify(runpodAdapter).getStatus();
        }

        @Test
        void delegatesGetConnectionDetails_toRunpodAdapter() {
            PodConnectionDetails details = new PodConnectionDetails("http://1.2.3.4:11434", null);
            when(runpodAdapter.getConnectionDetails()).thenReturn(Mono.just(details));

            StepVerifier.create(registry.getConnectionDetails())
                    .expectNext(details)
                    .verifyComplete();

            verify(runpodAdapter).getConnectionDetails();
        }
    }

    @Nested
    class WhenActiveProvider_HasNoRegisteredAdapter {

        @BeforeEach
        void setUp() {
            when(providerProperties.getActive()).thenReturn(null);
        }

        @Test
        void throwsProviderException_onStart() {
            assertThatThrownBy(() -> registry.start())
                    .isInstanceOf(ProviderException.class);
        }

        @Test
        void throwsProviderException_onStop() {
            assertThatThrownBy(() -> registry.stop())
                    .isInstanceOf(ProviderException.class);
        }

        @Test
        void throwsProviderException_onGetStatus() {
            assertThatThrownBy(() -> registry.getStatus())
                    .isInstanceOf(ProviderException.class);
        }

        @Test
        void throwsProviderException_onGetConnectionDetails() {
            assertThatThrownBy(() -> registry.getConnectionDetails())
                    .isInstanceOf(ProviderException.class);
        }
    }
}