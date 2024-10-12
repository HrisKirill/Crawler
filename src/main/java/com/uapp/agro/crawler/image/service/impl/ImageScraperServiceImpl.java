package com.uapp.agro.crawler.image.service.impl;

import com.uapp.agro.crawler.config.ApplicationProperties;
import com.uapp.agro.crawler.image.service.ImageInfoService;
import com.uapp.agro.crawler.image.service.ImageScraperService;
import com.uapp.agro.crawler.image.service.consumer.ImageScraperConsumer;
import com.uapp.agro.crawler.image.service.producer.ImageScraperProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Slf4j
@Service
public class ImageScraperServiceImpl implements ImageScraperService {
    private final ApplicationProperties properties;
    private final Integer maxProducerThreadCount;
    private final Integer maxConsumerThreadCount;
    private final BlockingQueue<String> images = new LinkedBlockingQueue<>();
    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    private final Set<String> visitedImages = ConcurrentHashMap.newKeySet();
    private final ConcurrentSkipListSet<String> urlQueue = new ConcurrentSkipListSet<>();
    private final ExecutorService executorService;
    private final ImageInfoService infoService;
    private final StopWatch stopWatch = new StopWatch();

    public ImageScraperServiceImpl(ApplicationProperties properties, ImageInfoService infoService) {
        this.properties = properties;
        this.maxProducerThreadCount = properties.getThreadProperties().maxProducerCount();
        this.maxConsumerThreadCount = properties.getThreadProperties().maxConsumerCount();
        this.executorService = Executors.newFixedThreadPool(maxProducerThreadCount + maxConsumerThreadCount);
        this.infoService = infoService;
    }

    @Override
    public void startScraping(String startUrl) {
        stopWatch.start();
        Long minImageSize = properties.getImageProperties().sizeBeforeCompressedKb();
        String folderPath = properties.getImageProperties().folderPath();
        int startProducerThreadCount = 1;
        AtomicInteger producersCount = new AtomicInteger(startProducerThreadCount);
        executorService.submit(new ImageScraperProducer(images, startUrl, minImageSize, urlQueue, visitedUrls, visitedImages, producersCount, executorService, maxProducerThreadCount));


        CompletableFuture[] futures = IntStream.rangeClosed(0, maxConsumerThreadCount)
                .mapToObj(i -> CompletableFuture.runAsync(
                        new ImageScraperConsumer(images,
                                folderPath,
                                infoService,
                                producersCount,
                                properties.getImageProperties().availableFormats()),
                        executorService
                ))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures)
                .whenComplete((unused, throwable) -> {
                    shutdownExecutorService();
                    stopWatch.stop();
                    log.info("Execution time: {} ms", stopWatch.getTotalTimeMillis());
                });
    }


    private void shutdownExecutorService() {
        executorService.shutdown();
        try {
            boolean terminated = executorService.awaitTermination(3, TimeUnit.SECONDS);
            if (!terminated) {
                log.info("Forcing shutdown...");
                executorService.shutdownNow();
            } else {
                log.info("ExecutorService successfully terminated.");
            }
        } catch (InterruptedException e) {
            log.info("Termination interrupted, forcing shutdown...");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
