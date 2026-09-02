package com.fse.flashsale.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Kafka topology owned by this service. */
@Configuration
public class KafkaConfig {

    public static final String FLASH_SALE_ORDERS_TOPIC = "flash-sale-orders";

    @Bean
    public NewTopic flashSaleOrdersTopic() {
        return TopicBuilder.name(FLASH_SALE_ORDERS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
