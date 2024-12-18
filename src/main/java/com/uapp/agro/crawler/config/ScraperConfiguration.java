package com.uapp.agro.crawler.config;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Set;

@Data
@Component
public class ScraperConfiguration {
    private final Integer maxProducerThreadCount;
    private final Integer maxConsumerThreadCount;
    private final Long minImageSize;
    private final String folderPath;
    private final Set<String> availableFormats;
    private final Long minUrlsGenerateProducer;

    public ScraperConfiguration(ApplicationProperties properties) {
        this.maxProducerThreadCount = properties.getThreadProperties().maxProducerCount();
        this.maxConsumerThreadCount = properties.getThreadProperties().maxConsumerCount();
        this.minImageSize = properties.getImageProperties().minSizeForScrapingKB();
        this.folderPath = properties.getImageProperties().folderPath();
        this.availableFormats = properties.getImageProperties().availableFormats();
        this.minUrlsGenerateProducer = properties.getProducerProperties().minUrlsGenerateProducer();
    }
}
