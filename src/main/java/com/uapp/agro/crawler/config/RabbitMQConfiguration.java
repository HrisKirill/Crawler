package com.uapp.agro.crawler.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfiguration {
    public static final String IMAGE_PROCESS_QUEUE = "process-image";
    public static final String IMAGE_EXCHANGE = "image-exchange";
    public static final String ROUTING_KEY = "routing-key";

    @Bean
    public Queue queue() {
        return new Queue(IMAGE_PROCESS_QUEUE, false);
    }

    @Bean
    public Exchange exchange() {
        return new DirectExchange(IMAGE_EXCHANGE);
    }

    @Bean
    public Binding binding(Queue queue, Exchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(ROUTING_KEY)
                .noargs();
    }
}
