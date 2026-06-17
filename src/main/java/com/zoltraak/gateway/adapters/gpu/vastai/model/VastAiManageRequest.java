package com.zoltraak.gateway.adapters.gpu.vastai.model;

public record VastAiManageRequest(
        String state,
        String label
) {
    public VastAiManageRequest(String state) {
        this(state, null);
    }
}
