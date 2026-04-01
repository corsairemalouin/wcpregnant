package org.manu.wcpregnant.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class ToiletteServiceConfig {
    @Value("${app.distance.max}")
    Double maxDistance = 100.0;
    @Value("${app.result.max}")
    Integer maxResult = 5;

}
