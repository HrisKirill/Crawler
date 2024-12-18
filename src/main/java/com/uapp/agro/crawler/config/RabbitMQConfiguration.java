package com.uapp.agro.crawler.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfiguration {
    public static final String IMAGES_QUEUE = "IMAGES.QUEUE";
    public static final String DLQ_IMAGES_QUEUE = "DLQ." + IMAGES_QUEUE;
    public static final String PARKING_LOT_IMAGES_QUEUE = "PARKING.LOT" + IMAGES_QUEUE;
    public static final String IMAGES_EXCHANGE = "IMAGES.EXCHANGE";
    public static final String PARKING_LOT_IMAGES_EXCHANGE = "PARKING.LOT" + IMAGES_EXCHANGE;
    public static final String DLX_IMAGES_EXCHANGE = "DLX." + IMAGES_EXCHANGE;
    public static final String ROUTING_KEY_IMAGES_QUEUE = "ROUTING_KEY_IMAGES_QUEUE";


    @Bean
    Queue messagesQueue() {
        return QueueBuilder.durable(IMAGES_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_IMAGES_EXCHANGE)
                .build();
    }

    @Bean
    DirectExchange messagesExchange() {
        return new DirectExchange(IMAGES_EXCHANGE);
    }

    @Bean
    Binding bindingMessages() {
        return BindingBuilder.bind(messagesQueue()).to(messagesExchange()).with(ROUTING_KEY_IMAGES_QUEUE);
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_IMAGES_QUEUE).build();
    }

    @Bean
    FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DLX_IMAGES_EXCHANGE);
    }

    @Bean
    Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange());
    }

    @Bean
    FanoutExchange parkingLotExchange() {
        return new FanoutExchange(PARKING_LOT_IMAGES_EXCHANGE);
    }

    @Bean
    Queue parkingLotQueue() {
        return QueueBuilder.durable(PARKING_LOT_IMAGES_QUEUE).build();
    }

    @Bean
    Binding parkingLotBinding() {
        return BindingBuilder.bind(parkingLotQueue()).to(parkingLotExchange());
    }
}
