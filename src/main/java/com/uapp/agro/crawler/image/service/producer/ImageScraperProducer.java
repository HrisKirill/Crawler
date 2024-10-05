package com.uapp.agro.crawler.image.service.producer;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ImageScraperProducer implements Runnable {
    private static final int MAX_PRODUCERS = 7;
    private final Long minimalImageSizeKb;
    private final BlockingQueue<String> images;
    private final ConcurrentSkipListSet<String> urlQueue;
    private final Set<String> visitedUrls;
    private final Set<String> visitedImages;
    private final AtomicInteger producersCount;
    private final ExecutorService producerPool;

    public ImageScraperProducer(
            BlockingQueue<String> images,
            String startUrl,
            Long minimalImageSizeKb,
            ConcurrentSkipListSet<String> urlQueue,
            Set<String> visitedUrls,
            Set<String> visitedImages,
            AtomicInteger producersCount,
            ExecutorService producerPool
    ) {
        this.images = images;
        this.minimalImageSizeKb = minimalImageSizeKb;
        this.urlQueue = urlQueue;
        this.visitedUrls = visitedUrls;
        this.visitedImages = visitedImages;
        this.producersCount = producersCount;
        this.producerPool = producerPool;
        this.urlQueue.add(startUrl);
    }

    @Override
    public void run() {
        try {
            findAllImages();
        } finally {
            producersCount.decrementAndGet();
            log.info("Producer count: {}", producersCount.get());
        }
    }

    public void findAllImages() {
        while (!urlQueue.isEmpty()) {
            try {
                if (isPossibleCreateNewProducer()) {
                    spawnNewProducer();
                }

                String currentUrl = poll();
                if (currentUrl == null) {
                    continue;
                }

                if (visitedUrls.contains(currentUrl)) {
                    log.info("Skip link: {}", currentUrl);
                    continue;
                }

                visitedUrls.add(currentUrl);

                log.info("Scan the page: {}", currentUrl);

                Document document = Jsoup.connect(currentUrl).get();
                findAndProcessImages(document);
                findAndProcessLinks(document);
            } catch (Exception e) {
                log.info(e.getMessage());
            }
        }
        log.info("{}: That`s all", Thread.currentThread().getName());
    }

    private boolean isPossibleCreateNewProducer() {
        return producersCount.get() < MAX_PRODUCERS && urlQueue.size() > 10;
    }

    private String poll() {
        try {
            String first = urlQueue.first();
            urlQueue.remove(first);
            return first;
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private void spawnNewProducer() {
        String nextUrl = poll();
        if (nextUrl != null) {
            producersCount.incrementAndGet();
            producerPool.submit(new ImageScraperProducer(images, nextUrl, minimalImageSizeKb, urlQueue, visitedUrls, visitedImages, producersCount, producerPool));
            log.info("Spawned new producer for URL: {}", nextUrl);
        }
    }


    private void findAndProcessImages(Document document) {
        final Elements images = document.select("img");
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
        final String[] srcsetImages = srcset.split(",");

        for (String srcItem : srcsetImages) {
            String[] parts = srcItem.trim().split("\\s+");
            String imageUrl = parts[0];
            processImage(imageUrl);
        }
    }

    private void processImage(String imageUrl) {
        final Long imageSize = getImageSize(imageUrl);
        if (isImageSizeValid(imageSize)
                && !visitedImages.contains(imageUrl)
                && images.add(imageUrl)) {
            log.info("Adding image: {} with size: {} KB", imageUrl, imageSize);
            visitedImages.add(imageUrl);
        }
    }

    private boolean isImageSizeValid(Long imageSize) {
        return imageSize >= minimalImageSizeKb;
    }

    private Long getImageSize(String imageUrl) {
        try {
            final URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();

            final Long contentLengthInKb = connection.getContentLengthLong() / 1024;
            connection.disconnect();
            return contentLengthInKb;
        } catch (IOException e) {
            log.info("Error getting image size: {}", e.getMessage());
            return -1L;
        }
    }

    private void findAndProcessLinks(Document document) {
        final Elements links = document.select("section a[href]");
        for (Element link : links) {
            final String nextUrl = link.absUrl("href");
            if (!visitedUrls.contains(nextUrl) && urlQueue.add(nextUrl)) {
                log.info("Added new scan link: {}", nextUrl);
            }
        }
    }


}
