package com.uapp.agro.crawler.consumer.manager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public interface ConsumerManager {
    CompletableFuture<Void> startConsumers(AtomicInteger producersCount);
}
