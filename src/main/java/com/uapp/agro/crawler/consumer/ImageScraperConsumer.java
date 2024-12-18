package com.uapp.agro.crawler.consumer;

import org.springframework.amqp.core.Message;

import java.io.IOException;

public interface ImageScraperConsumer {
    void processFailedMessages(Message failedMessage);

    void saveImage(String imageUrl) throws IOException;
}
