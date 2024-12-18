package com.uapp.agro.crawler.scraper.service.impl;

import com.uapp.agro.crawler.config.ApplicationProperties;
import com.uapp.agro.crawler.config.ScraperConfiguration;
import com.uapp.agro.crawler.producer.manager.ProducerManager;
import com.uapp.agro.crawler.producer.manager.managerImpl.ProducerManagerImpl;
import com.uapp.agro.crawler.scraper.service.ImageScraperService;
import com.uapp.agro.crawler.scraper.util.ExecutorServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ImageScraperServiceImpl implements ImageScraperService {
    private final ExecutorService executorService;
    private final ProducerManager producerManager;

    public ImageScraperServiceImpl(
            ApplicationProperties properties,
            RabbitTemplate rabbitTemplate
    ) {
        ScraperConfiguration config = new ScraperConfiguration(properties);
        this.executorService = Executors.newFixedThreadPool(config.getMaxProducerThreadCount() + config.getMaxConsumerThreadCount());
        this.producerManager = new ProducerManagerImpl(config, executorService, rabbitTemplate);
    }

    @Override
    public void startScraping(String startUrl) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        producerManager.startProducer(startUrl);
        stopWatch.stop();
        log.info("Execution time: {} ms", stopWatch.getTotalTimeMillis());
    }
}
