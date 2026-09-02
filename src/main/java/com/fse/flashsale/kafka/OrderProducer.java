package com.fse.flashsale.kafka;

import com.fse.flashsale.config.KafkaConfig;
import com.fse.flashsale.dto.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes the order command after Redis has atomically reserved stock. */
@Component
public class OrderProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderProducer.class);

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderProducer(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(OrderEvent event) {
        kafkaTemplate.send(KafkaConfig.FLASH_SALE_ORDERS_TOPIC, event.getOrderCode(), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Unable to publish flash-sale order {}", event.getOrderCode(), exception);
                    } else {
                        log.debug("Published flash-sale order {} to partition {}", event.getOrderCode(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
