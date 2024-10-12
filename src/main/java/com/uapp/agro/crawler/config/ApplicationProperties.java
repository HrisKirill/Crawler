package com.uapp.agro.crawler.config;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("application")
@RequiredArgsConstructor
@Data
public class ApplicationProperties {
    private final ImageProperties imageProperties;
    private final ThreadProperties threadProperties;

    public record ImageProperties(
            String folderPath,
            Long sizeBeforeCompressedKb,
            List<String> availableFormats) {
    }

    public record ThreadProperties(Integer maxProducerCount,
                                   Integer maxConsumerCount) {
    }
}
