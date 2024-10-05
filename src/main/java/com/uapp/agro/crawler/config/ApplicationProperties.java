package com.uapp.agro.crawler.config;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("application")
@RequiredArgsConstructor
@Data
public class ApplicationProperties {
    private final ImageProperties imageProperties;

    public record ImageProperties(String folderPath, Long sizeBeforeCompressedKb) {
    }
}
