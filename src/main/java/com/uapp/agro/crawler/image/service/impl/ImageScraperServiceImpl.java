package com.uapp.agro.crawler.image.service.impl;

import com.uapp.agro.crawler.config.ApplicationProperties;
import com.uapp.agro.crawler.image.service.ImageInfoService;
import com.uapp.agro.crawler.image.service.ImageScraperService;
import com.uapp.agro.crawler.image.service.consumer.ImageScraperConsumer;
import com.uapp.agro.crawler.image.service.producer.ImageScraperProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageScraperServiceImpl implements ImageScraperService {
    private final ApplicationProperties properties;
    private final BlockingQueue<String> images = new LinkedBlockingQueue<>();
    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    private final Set<String> visitedImages = ConcurrentHashMap.newKeySet();
    private final ConcurrentSkipListSet<String> urlQueue = new ConcurrentSkipListSet<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final ImageInfoService infoService;
    private final StopWatch stopWatch = new StopWatch();

    @Override
    public void startScraping(String startUrl) {
        stopWatch.start();
        final Long minImageSize = properties.getImageProperties().sizeBeforeCompressedKb();
        final String folderPath = properties.getImageProperties().folderPath();
        final int producerCount = 1;
        final int consumerCount = 3;

        AtomicInteger producersCount = new AtomicInteger(producerCount);
        executorService.submit(new ImageScraperProducer(images, startUrl, minImageSize, urlQueue, visitedUrls, visitedImages, producersCount, executorService));


        CompletableFuture[] futures = IntStream.rangeClosed(0, consumerCount)
                .mapToObj(i -> CompletableFuture.runAsync(
                        new ImageScraperConsumer(images, folderPath, infoService, producersCount),
                        executorService
                ))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures)
                .whenComplete((unused, throwable) -> {
                    stopWatch.stop();
                    log.info("Execution time: {} ms", stopWatch.getTotalTimeMillis());
                    shutdownExecutorService();
                });
    }


    private void shutdownExecutorService() {
        executorService.shutdown();
        try {
            boolean terminated = executorService.awaitTermination(30, TimeUnit.SECONDS);
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
