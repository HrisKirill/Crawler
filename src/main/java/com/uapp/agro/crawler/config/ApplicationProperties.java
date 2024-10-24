package com.uapp.agro.crawler.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties("application")
@RequiredArgsConstructor
@Data
public class ApplicationProperties {
    private final ImageProperties imageProperties;
    private final ThreadProperties threadProperties;

    public record ImageProperties(
            @Min(1) Long minCompressedImageSize,
            @NotBlank String folderPath,
            @Min(1) Long minSizeBeforeCompressedKb,
            @NotEmpty Set<String> availableFormats) {
    }

    public record ThreadProperties(@Min(1) Integer maxProducerCount,
                                   @Min(1) Integer maxConsumerCount) {
    }
}
