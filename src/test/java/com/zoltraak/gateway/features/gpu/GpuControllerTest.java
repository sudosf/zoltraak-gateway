package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.config.SecurityConfig;
import com.zoltraak.gateway.domain.enums.GpuProviderType;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.features.gpu.model.ProviderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(GpuController.class)
@Import(SecurityConfig.class)
class GpuControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private GpuLifecycleManager gpuLifecycleManager;

    @Test
    void getStatus_returns200_withCurrentStatus() {
        when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.READY);

        webTestClient.get()
                .uri(GpuController.BASE_PATH + GpuController.STATUS)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data").isEqualTo("READY");
    }

    @Test
    void start_delegatesToLifecycleManager_andReturns200() {
        when(gpuLifecycleManager.requestStart()).thenReturn(Mono.empty());

        webTestClient.post()
                .uri(GpuController.BASE_PATH + GpuController.START)
                .exchange()
                .expectStatus().isOk()
                .expectBody();

        verify(gpuLifecycleManager).requestStart();
    }

    @Test
    void stop_delegatesToLifecycleManager_andReturns200() {
        when(gpuLifecycleManager.requestShutdown()).thenReturn(Mono.empty());

        webTestClient.post()
                .uri(GpuController.BASE_PATH + GpuController.STOP)
                .exchange()
                .expectStatus().isOk()
                .expectBody();

        verify(gpuLifecycleManager).requestShutdown();
    }

    @Test
    void switchProvider_delegatesToLifecycleManager_andReturns200() {
        ProviderRequest request = new ProviderRequest(GpuProviderType.RUNPOD);

        webTestClient.post()
                .uri(GpuController.BASE_PATH + GpuController.PROVIDER)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody();

        verify(gpuLifecycleManager).switchProvider(request);
    }
}
