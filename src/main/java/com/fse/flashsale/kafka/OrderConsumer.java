package com.fse.flashsale.kafka;

import com.fse.flashsale.dto.OrderEvent;
import com.fse.flashsale.entity.Order;
import com.fse.flashsale.entity.Product;
import com.fse.flashsale.entity.Voucher;
import com.fse.flashsale.repository.OrderRepository;
import com.fse.flashsale.repository.ProductRepository;
import com.fse.flashsale.repository.VoucherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Applies accepted Redis reservations to PostgreSQL with row-level serialization. */
@Component
public class OrderConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);
    private final OrderRepository orders;
    private final ProductRepository products;
    private final VoucherRepository vouchers;

    public OrderConsumer(OrderRepository orders, ProductRepository products, VoucherRepository vouchers) {
        this.orders = orders;
        this.products = products;
        this.vouchers = vouchers;
    }

    @Transactional
    @KafkaListener(topics = "flash-sale-orders", groupId = "flash-sale-group")
    public void consume(OrderEvent event) {
        validate(event);
        if (orders.findByOrderCode(event.getOrderCode()).isPresent()) {
            log.info("Ignoring already persisted order event: {}", event.getOrderCode());
            return;
        }

        Product product = products.findByIdForUpdate(event.getProductId())
                .orElseThrow(() -> new IllegalStateException("Product does not exist: " + event.getProductId()));
        if (product.getStock() < event.getQuantity()) {
            throw new IllegalStateException("PostgreSQL stock is inconsistent for product " + event.getProductId());
        }

        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(event.getQuantity()));
        BigDecimal discount = BigDecimal.ZERO;
        String voucherCode = normalize(event.getVoucherCode());
        if (voucherCode != null) {
            if (orders.existsByUserIdAndVoucherCodeAndStatus(event.getUserId(), voucherCode, "SUCCESS")) {
                throw new IllegalStateException("Voucher was already used by this user: " + voucherCode);
            }
            Voucher voucher = vouchers.findByCodeForUpdate(voucherCode)
                    .orElseThrow(() -> new IllegalStateException("Voucher does not exist: " + voucherCode));
            if (voucher.getQuota() <= 0) {
                throw new IllegalStateException("PostgreSQL voucher quota is inconsistent: " + voucherCode);
            }
            voucher.setQuota(voucher.getQuota() - 1);
            discount = voucher.getDiscountAmount().min(total);
        }

        product.setStock(product.getStock() - event.getQuantity());
        Order order = Order.builder()
                .orderCode(event.getOrderCode())
                .userId(event.getUserId())
                .productId(event.getProductId())
                .quantity(event.getQuantity())
                .totalAmount(total)
                .voucherCode(voucherCode)
                .discountAmount(discount)
                .finalAmount(total.subtract(discount))
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();
        orders.save(order);
        log.info("Persisted order {} (product={}, voucher={})", order.getOrderCode(), product.getId(), voucherCode);
    }

    private void validate(OrderEvent event) {
        if (event == null || event.getOrderCode() == null || event.getOrderCode().isBlank()
                || event.getProductId() == null || event.getUserId() == null
                || event.getQuantity() == null || event.getQuantity() <= 0) {
            throw new IllegalArgumentException("Malformed flash-sale order event");
        }
    }

    private String normalize(String voucherCode) {
        return voucherCode == null || voucherCode.isBlank() ? null : voucherCode.trim();
    }
}
