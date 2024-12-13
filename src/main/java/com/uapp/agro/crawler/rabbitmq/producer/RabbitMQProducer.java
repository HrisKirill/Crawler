package com.uapp.agro.crawler.rabbitmq.producer;

public interface RabbitMQProducer {
    void sendMessage(String exchange, String routingKey, String message);
}
