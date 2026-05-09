package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.adapters.gpu.GpuProviderPort;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.shared.GatewayResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/gpu")
public class GpuController {

    private final GpuProviderPort gpuProviderPort;

    public GpuController(GpuProviderPort gpuProviderPort) {
        this.gpuProviderPort = gpuProviderPort;
    }

    @GetMapping("/status")
    public Mono<ResponseEntity<GatewayResponse<PodStatus>>> getStatus() {
        return gpuProviderPort.getStatus()
                .map(status -> ResponseEntity.ok(GatewayResponse.success(status)));
    }

    @PostMapping("/start")
    public Mono<ResponseEntity<GatewayResponse<Void>>> start() {
        return gpuProviderPort.start()
                .then(Mono.just(ResponseEntity.ok(GatewayResponse.success(null))));
    }

    @PostMapping("/stop")
    public Mono<ResponseEntity<GatewayResponse<Void>>> stop() {
        return gpuProviderPort.stop()
                .then(Mono.just(ResponseEntity.ok(GatewayResponse.success(null))));
    }
}