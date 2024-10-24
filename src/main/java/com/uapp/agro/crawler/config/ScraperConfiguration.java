package com.uapp.agro.crawler.config;

import lombok.Data;

import java.util.Set;

@Data
public class ScraperConfiguration {
    private final Integer maxProducerThreadCount;
    private final Integer maxConsumerThreadCount;
    private final Long minImageSize;
    private final String folderPath;
    private final Long minCompressedSize;
    private final Set<String> availableFormats;

    public ScraperConfiguration(ApplicationProperties properties) {
        this.maxProducerThreadCount = properties.getThreadProperties().maxProducerCount();
        this.maxConsumerThreadCount = properties.getThreadProperties().maxConsumerCount();
        this.minImageSize = properties.getImageProperties().minSizeBeforeCompressedKb();
        this.folderPath = properties.getImageProperties().folderPath();
        this.minCompressedSize = properties.getImageProperties().minCompressedImageSize();
        this.availableFormats = properties.getImageProperties().availableFormats();
    }
}
