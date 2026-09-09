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

/** Hot-path HTTP adapter. Database writes are intentionally delegated to Kafka. */
@RestController
@RequestMapping("/api/v1/flash-sale")
public class FlashSaleController {
    private final RedisStockService redis;
    private final OrderProducer producer;

    public FlashSaleController(RedisStockService redis, OrderProducer producer) {
        this.redis = redis;
        this.producer = producer;
    }

    @PostMapping("/init-stock")
    public ResponseEntity<Void> initStock(@Valid @RequestBody InitStockRequest request) {
        redis.initStock(request.getProductId(), request.getStock());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/order")
    public ResponseEntity<?> placeOrder(@Valid @RequestBody FlashSaleRequest request) {
        String voucherCode = normalize(request.getVoucherCode());
        long reservation = redis.reserve(request.getProductId(), request.getUserId(), request.getQuantity(), voucherCode);
        if (reservation != RedisStockService.RESERVED_WITH_VOUCHER
                && reservation != RedisStockService.RESERVED_WITHOUT_VOUCHER) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(failureMessage(reservation));
        }

        String orderCode = "FS-" + UUID.randomUUID();
        producer.publish(OrderEvent.builder()
                .orderCode(orderCode)
                .productId(request.getProductId())
                .userId(request.getUserId())
                .quantity(request.getQuantity())
                .voucherCode(voucherCode)
                .build());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new OrderAcceptedResponse(orderCode));
    }

    private String failureMessage(long code) {
        return switch ((int) code) {
            case 0 -> "Out of stock";
            case -1 -> "Voucher expired";
            case -2 -> "Voucher already redeemed";
            case -3 -> "Invalid product or voucher";
            default -> "Unable to reserve order resources";
        };
    }

    private String normalize(String code) {
        return code == null || code.isBlank() ? null : code.trim();
    }
}
