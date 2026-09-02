package com.fse.flashsale.kafka;

import com.fse.flashsale.dto.OrderEvent;
import com.fse.flashsale.entity.Order;
import com.fse.flashsale.entity.Product;
import com.fse.flashsale.repository.OrderRepository;
import com.fse.flashsale.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Persists Kafka events. The order code makes at-least-once delivery idempotent. */
@Component
public class OrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderConsumer(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    @KafkaListener(topics = "flash-sale-orders", groupId = "flash-sale-group")
    public void consume(OrderEvent event) {
        if (event == null || event.getOrderCode() == null || event.getOrderCode().isBlank()) {
            throw new IllegalArgumentException("Order event must contain an orderCode");
        }

        if (orderRepository.findByOrderCode(event.getOrderCode()).isPresent()) {
            log.debug("Ignoring duplicate order event {}", event.getOrderCode());
            return;
        }

        Product product = productRepository.findById(event.getProductId()).orElse(null);
        boolean productIsAvailable = product != null;
        BigDecimal totalAmount = productIsAvailable
                ? product.getPrice().multiply(BigDecimal.valueOf(event.getQuantity()))
                : BigDecimal.ZERO;

        Order order = Order.builder()
                .orderCode(event.getOrderCode())
                .userId(event.getUserId())
                .productId(event.getProductId())
                .quantity(event.getQuantity())
                .totalAmount(totalAmount)
                .status(productIsAvailable ? SUCCESS : FAILED)
                .createdAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);
        log.info("Persisted flash-sale order {} with status {}", order.getOrderCode(), order.getStatus());
    }
}
