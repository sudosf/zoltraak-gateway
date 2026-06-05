package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.response.GatewayResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(GpuController.BASE_PATH)
public class GpuController {

    public static final String BASE_PATH = "/api/v1/gpu";
    public static final String STATUS = "/status";
    public static final String START = "/start";
    public static final String STOP = "/stop";

    private final GpuLifecycleManager gpuLifecycleManager;

    public GpuController(GpuLifecycleManager gpuLifecycleManager) {
        this.gpuLifecycleManager = gpuLifecycleManager;
    }

    @GetMapping(STATUS)
    public ResponseEntity<GatewayResponse<PodStatus>> getStatus() {
        PodStatus status = gpuLifecycleManager.getStatus();
        return ResponseEntity.ok(GatewayResponse.success(status));
    }

    @PostMapping(START)
    public Mono<ResponseEntity<GatewayResponse<Void>>> start() {
        return gpuLifecycleManager.requestStart()
                .then(Mono.just(ResponseEntity.ok(GatewayResponse.success(null))));
    }

    @PostMapping(STOP)
    public Mono<ResponseEntity<GatewayResponse<Void>>> stop() {
        return gpuLifecycleManager.requestShutdown()
                .then(Mono.just(ResponseEntity.ok(GatewayResponse.success(null))));
    }
}
