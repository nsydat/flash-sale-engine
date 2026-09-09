package com.fse.flashsale.service;

import com.fse.flashsale.dto.CampaignInitRequest;
import com.fse.flashsale.dto.CampaignInitResponse;
import com.fse.flashsale.dto.CampaignVerificationResponse;
import com.fse.flashsale.entity.Product;
import com.fse.flashsale.entity.Voucher;
import com.fse.flashsale.repository.OrderRepository;
import com.fse.flashsale.repository.ProductRepository;
import com.fse.flashsale.repository.VoucherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administrative setup and reconciliation operations, intentionally outside the hot path. */
@Service
public class CampaignService {
    private final ProductRepository products;
    private final VoucherRepository vouchers;
    private final OrderRepository orders;
    private final RedisStockService redis;

    public CampaignService(ProductRepository products, VoucherRepository vouchers, OrderRepository orders, RedisStockService redis) {
        this.products = products;
        this.vouchers = vouchers;
        this.orders = orders;
        this.redis = redis;
    }

    @Transactional
    public CampaignInitResponse initialize(CampaignInitRequest request) {
        Product product = products.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product does not exist: " + request.productId()));
        product.setStock(request.stock());
        product.setPrice(request.price());

        Voucher voucher = vouchers.findByCode(request.voucherCode()).orElseGet(Voucher::new);
        voucher.setCode(request.voucherCode().trim());
        voucher.setQuota(request.voucherQuota());
        voucher.setDiscountAmount(request.discountAmount());
        vouchers.save(voucher);
        products.flush();
        vouchers.flush();

        redis.initializeCampaign(product.getId(), request.stock(), voucher.getCode(), request.voucherQuota());
        return new CampaignInitResponse(product.getId(), voucher.getCode(), request.stock(), request.voucherQuota());
    }

    @Transactional(readOnly = true)
    public CampaignVerificationResponse verify(Long productId) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product does not exist: " + productId));
        String voucherCode = redis.voucherCodeForProduct(productId);
        long databaseOrders = defaultLong(orders.totalSuccessfulQuantityByProductId(productId));
        long redisStockDeductions = difference(redis.initialStock(productId), redis.currentStock(productId));

        long databaseRedemptions = 0;
        long globalVoucherRedemptions = 0;
        long distinctVoucherUsers = 0;
        long redisVoucherDeductions = 0;
        boolean voucherQuotaNonNegative = true;
        if (voucherCode != null) {
            databaseRedemptions = orders.countByProductIdAndVoucherCodeAndStatus(productId, voucherCode, "SUCCESS");
            globalVoucherRedemptions = orders.countByVoucherCodeAndStatus(voucherCode, "SUCCESS");
            distinctVoucherUsers = orders.countDistinctVoucherUsers(voucherCode);
            redisVoucherDeductions = difference(redis.initialVoucherQuota(voucherCode), redis.currentVoucherQuota(voucherCode));
            voucherQuotaNonNegative = vouchers.findByCode(voucherCode).map(v -> v.getQuota() >= 0).orElse(false);
        }

        boolean zeroOverselling = product.getStock() >= 0 && voucherQuotaNonNegative;
        boolean zeroDuplicateVoucherUsage = globalVoucherRedemptions == distinctVoucherUsers;
        boolean exactMatch = databaseOrders == redisStockDeductions
                && databaseRedemptions == redisVoucherDeductions;
        return new CampaignVerificationResponse(productId, voucherCode, databaseOrders, redisStockDeductions,
                databaseRedemptions, redisVoucherDeductions, zeroOverselling, zeroDuplicateVoucherUsage,
                zeroOverselling && zeroDuplicateVoucherUsage && exactMatch);
    }

    private long defaultLong(Long value) { return value == null ? 0L : value; }
    private long difference(long initial, long current) { return initial < 0 || current < 0 ? -1L : initial - current; }
}
