package org.manu.wcpregnant.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class ToiletteServiceConfig {

    // Si 'app.distance.max' n'est pas dans properties, utilise 100.0
    @Value("${app.distance.max:100.0}")
    private Double maxDistance;

    // Si 'app.result.max' n'est pas dans properties, utilise 5
    @Value("${app.result.max:5}")
    private Integer maxResult;
}