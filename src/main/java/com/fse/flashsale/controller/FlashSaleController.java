package com.fse.flashsale.controller;

import com.fse.flashsale.dto.FlashSaleRequest;
import com.fse.flashsale.dto.InitStockRequest;
import com.fse.flashsale.dto.OrderAcceptedResponse;
import com.fse.flashsale.dto.OrderEvent;
import com.fse.flashsale.kafka.OrderProducer;
import com.fse.flashsale.service.RedisStockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** HTTP adapter for the flash-sale reservation workflow. */
@RestController
@RequestMapping("/api/v1/flash-sale")
public class FlashSaleController {

    private final RedisStockService redisStockService;
    private final OrderProducer orderProducer;

    public FlashSaleController(RedisStockService redisStockService, OrderProducer orderProducer) {
        this.redisStockService = redisStockService;
        this.orderProducer = orderProducer;
    }

    @PostMapping("/init-stock")
    public ResponseEntity<Void> initStock(@Valid @RequestBody InitStockRequest request) {
        redisStockService.initStock(request.getProductId(), request.getStock());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/order")
    public ResponseEntity<?> placeOrder(@Valid @RequestBody FlashSaleRequest request) {
        long result = redisStockService.deductStock(request.getProductId(), request.getQuantity());
        if (result != 1L) {
            String message = result == -1L ? "Product stock has not been initialized" : "Product is out of stock";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
        }

        String orderCode = "FS-" + UUID.randomUUID();
        orderProducer.publish(OrderEvent.builder()
                .orderCode(orderCode)
                .userId(request.getUserId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .build());

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new OrderAcceptedResponse(orderCode));
    }
}
