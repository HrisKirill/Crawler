package com.uapp.agro.crawler.scraper.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class ExecutorServiceUtil {
    public static void shutdownGracefully(ExecutorService executorService, long timeout, TimeUnit unit) {
        executorService.shutdown();
        try {
            boolean terminated = executorService.awaitTermination(timeout, unit);
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
