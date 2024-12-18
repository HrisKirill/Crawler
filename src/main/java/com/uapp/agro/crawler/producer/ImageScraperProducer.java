package com.uapp.agro.crawler.producer;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static com.uapp.agro.crawler.config.RabbitMQConfiguration.*;

@Slf4j
public class ImageScraperProducer implements Runnable {
    private final RabbitTemplate rabbitTemplate;
    private final Integer maxProducers;
    private final Long minimalImageSizeKb;
    private final ConcurrentSkipListSet<String> urlQueue;
    private final Set<String> visitedUrls, visitedImages;
    private final AtomicInteger producersCount;
    private final ExecutorService producerPool;
    private final Long minUrlsGenerateProducer;

    public ImageScraperProducer(String startUrl, RabbitTemplate rabbitTemplate, Long minimalImageSizeKb, ConcurrentSkipListSet<String> urlQueue, Set<String> visitedUrls, Set<String> visitedImages, AtomicInteger producersCount, ExecutorService producerPool, Integer maxProducers, Long minUrlsGenerateProducer) {
        this.rabbitTemplate = rabbitTemplate;
        this.minimalImageSizeKb = minimalImageSizeKb;
        this.urlQueue = urlQueue;
        this.visitedUrls = visitedUrls;
        this.visitedImages = visitedImages;
        this.producersCount = producersCount;
        this.producerPool = producerPool;
        this.maxProducers = maxProducers;
        this.minUrlsGenerateProducer = minUrlsGenerateProducer;
        this.urlQueue.add(startUrl);
    }

    @Override
    public void run() {
        try {
            findAllImages();
        } finally {
            decrementProducersCount();
        }
    }

    public void findAllImages() {
        while (!urlQueue.isEmpty()) {
            try {
                spawnNewProducerIfNeeded();
                String currentUrl = urlQueue.pollFirst();
                if (currentUrl == null || !visitedUrls.add(currentUrl)) {
                    continue;
                }

                try {
                    log.info("Scan the page: {}", currentUrl);
                    Document document = Jsoup.connect(currentUrl).get();
                    findAndProcessImages(document);
                    findAndProcessLinks(document);
                } catch (Exception e) {
                    visitedUrls.remove(currentUrl);
                    log.warn("Problem with {} url processing", currentUrl);
                }

            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        }
        log.info("{}: That`s all", Thread.currentThread().getName());
    }

    private void spawnNewProducerIfNeeded() {
        if (shouldSpawnNewProducer()) {
            String nextUrl = urlQueue.pollFirst();
            if (nextUrl != null) {
                producersCount.incrementAndGet();
                producerPool.submit(new ImageScraperProducer(nextUrl, rabbitTemplate, minimalImageSizeKb, urlQueue, visitedUrls, visitedImages, producersCount, producerPool, maxProducers, minUrlsGenerateProducer));
                log.info("Spawned new producer for URL: {}", nextUrl);
            }
        }
    }

    private boolean shouldSpawnNewProducer() {
        return producersCount.get() < maxProducers && urlQueue.size() > minUrlsGenerateProducer;
    }


    private void findAndProcessImages(Document document) {
        Elements images = document.select("img");
        for (Element img : images) {
            String srcset = img.attr("srcset");
            if (!srcset.isEmpty()) {
                processSrcset(srcset);
            } else {
                String imgUrl = img.absUrl("src");
                processImage(imgUrl);
            }
        }
    }

    private void processSrcset(String srcset) {
        String[] srcsetImages = srcset.split(",");

        for (String srcItem : srcsetImages) {
            String[] parts = srcItem.trim().split("\\s+");
            String imageUrl = parts[0];
            processImage(imageUrl);
        }
    }

    private void processImage(String imageUrl) {
        Long imageSize = getImageSize(imageUrl);
        if (isImageSizeValid(imageSize)
                && visitedImages.add(imageUrl)) {
            rabbitTemplate.convertAndSend(IMAGES_EXCHANGE, ROUTING_KEY_IMAGES_QUEUE, imageUrl);
            log.info("Adding image: {} with size: {} KB", imageUrl, imageSize);
        }
    }

    private boolean isImageSizeValid(Long imageSize) {
        return imageSize >= minimalImageSizeKb;
    }

    private Long getImageSize(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();

            Long contentLengthInKb = connection.getContentLengthLong() / 1024;
            connection.disconnect();
            return contentLengthInKb;
        } catch (IOException e) {
            log.warn("Error getting image size: {}", e.getMessage());
            return -1L;
        }
    }

    private void findAndProcessLinks(Document document) {
        Elements links = document.select("section a[href]");
        for (Element link : links) {
            String nextUrl = link.absUrl("href");
            if (!visitedUrls.contains(nextUrl) && urlQueue.add(nextUrl)) {
                log.info("Added new scan link: {}", nextUrl);
            }
        }
    }

    private void decrementProducersCount() {
        int remaining = producersCount.decrementAndGet();
        log.info("Producer count: {}", remaining);
    }
}
