package com.zoltraak.gateway.adapters.gpu.runpod;

import com.zoltraak.gateway.adapters.gpu.runpod.model.RunpodCreatePodRequest;
import com.zoltraak.gateway.config.properties.ProviderProperties;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RunpodAdapterMapper {
    RunpodCreatePodRequest toRunpodCreateRequest(ProviderProperties.RunPodConfig.CreateProperties createProperties);
}
