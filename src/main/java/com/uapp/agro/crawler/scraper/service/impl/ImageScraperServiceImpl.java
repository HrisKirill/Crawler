package com.uapp.agro.crawler.scraper.service.impl;

import com.uapp.agro.crawler.config.ApplicationProperties;
import com.uapp.agro.crawler.config.ScraperConfiguration;
import com.uapp.agro.crawler.consumer.manager.ConsumerManager;
import com.uapp.agro.crawler.consumer.manager.managerImpl.ConsumerManagerImpl;
import com.uapp.agro.crawler.image.service.ImageInfoService;
import com.uapp.agro.crawler.producer.manager.ProducerManager;
import com.uapp.agro.crawler.producer.manager.managerImpl.ProducerManagerImpl;
import com.uapp.agro.crawler.scraper.service.ImageScraperService;
import com.uapp.agro.crawler.scraper.util.ExecutorServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ImageScraperServiceImpl implements ImageScraperService {
    private final ExecutorService executorService;
    private final ProducerManager producerManager;
    private final ConsumerManager consumerManager;

    public ImageScraperServiceImpl(ApplicationProperties properties, ImageInfoService infoService) {
        ScraperConfiguration config = new ScraperConfiguration(properties);
        this.executorService = Executors.newFixedThreadPool(config.getMaxProducerThreadCount() + config.getMaxConsumerThreadCount());
        this.producerManager = new ProducerManagerImpl(config, executorService);
        this.consumerManager = new ConsumerManagerImpl(config, infoService, executorService, producerManager.getImages());
    }

    @Override
    public void startScraping(String startUrl) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        producerManager.startProducer(startUrl);
        CompletableFuture<Void> consumersFuture = consumerManager.startConsumers(producerManager.getProducersCount());

        consumersFuture.whenComplete((unused, throwable) -> {
            shutdownExecutorService();
            stopWatch.stop();
            log.info("Execution time: {} ms", stopWatch.getTotalTimeMillis());
        });
    }

    private void shutdownExecutorService() {
        ExecutorServiceUtil.shutdownGracefully(executorService, 3, TimeUnit.SECONDS);
    }

}
