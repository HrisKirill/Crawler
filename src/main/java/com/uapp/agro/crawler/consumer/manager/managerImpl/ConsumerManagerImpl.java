package com.uapp.agro.crawler.consumer.manager.managerImpl;

import com.uapp.agro.crawler.config.ScraperConfiguration;
import com.uapp.agro.crawler.consumer.ImageScraperConsumer;
import com.uapp.agro.crawler.consumer.manager.ConsumerManager;
import com.uapp.agro.crawler.image.service.ImageInfoService;
import lombok.RequiredArgsConstructor;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class ConsumerManagerImpl implements ConsumerManager {
    private final ScraperConfiguration config;
    private final ImageInfoService infoService;
    private final ExecutorService executorService;
    private final BlockingQueue<String> imageQueue;
    private final Set<String> processedImages = ConcurrentHashMap.newKeySet();

    @Override
    public CompletableFuture<Void> startConsumers(AtomicInteger producersCount) {
        CompletableFuture[] futures = IntStream.rangeClosed(0, config.getMaxConsumerThreadCount())
                .mapToObj(i -> CompletableFuture.runAsync(
                        new ImageScraperConsumer(imageQueue,
                                processedImages,
                                config.getFolderPath(),
                                infoService,
                                producersCount,
                                config.getAvailableFormats(),
                                config.getMinCompressedSize()),
                        executorService
                ))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures);
    }
}
