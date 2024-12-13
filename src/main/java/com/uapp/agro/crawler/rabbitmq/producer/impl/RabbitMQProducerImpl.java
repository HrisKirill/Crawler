package com.uapp.agro.crawler.rabbitmq.producer.impl;

import com.uapp.agro.crawler.rabbitmq.producer.RabbitMQProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitMQProducerImpl implements RabbitMQProducer {
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void sendMessage(String exchange, String routingKey, String message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
