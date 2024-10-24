package com.uapp.agro.crawler.producer.manager.managerImpl;

import com.uapp.agro.crawler.config.ScraperConfiguration;
import com.uapp.agro.crawler.producer.ImageScraperProducer;
import com.uapp.agro.crawler.producer.manager.ProducerManager;
import lombok.RequiredArgsConstructor;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class ProducerManagerImpl implements ProducerManager {
    private final BlockingQueue<String> images = new LinkedBlockingQueue<>();
    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    private final Set<String> visitedImages = ConcurrentHashMap.newKeySet();
    private final ConcurrentSkipListSet<String> urlQueue = new ConcurrentSkipListSet<>();
    private final AtomicInteger producersCount = new AtomicInteger(1);
    private final ScraperConfiguration config;
    private final ExecutorService executorService;


    @Override
    public void startProducer(String startUrl) {
        executorService.submit(new ImageScraperProducer(images, startUrl, config.getMinImageSize(),
                urlQueue, visitedUrls, visitedImages, producersCount, executorService, config.getMaxProducerThreadCount()));
    }

    @Override
    public AtomicInteger getProducersCount() {
        return producersCount;
    }

    @Override
    public BlockingQueue<String> getImages() {
        return images;
    }
}



