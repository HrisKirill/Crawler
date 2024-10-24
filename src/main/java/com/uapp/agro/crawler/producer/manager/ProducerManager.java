package com.uapp.agro.crawler.producer.manager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public interface ProducerManager {

    void startProducer(String startUrl);

    AtomicInteger getProducersCount();

    BlockingQueue<String> getImages();
}
