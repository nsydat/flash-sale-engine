package com.fse.flashsale.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/** Redis reservation boundary for product inventory and one-time vouchers. */
@Service
public class RedisStockService {
    public static final long RESERVED_WITH_VOUCHER = 1L;
    public static final long RESERVED_WITHOUT_VOUCHER = 2L;
    public static final long PRODUCT_OUT_OF_STOCK = 0L;
    public static final long VOUCHER_QUOTA_EXHAUSTED = -1L;
    public static final long VOUCHER_ALREADY_USED = -2L;
    public static final long INVALID_RESOURCE = -3L;

    private static final String STOCK = "product:stock:";
    private static final String QUOTA = "voucher:quota:";
    private static final String USED = "voucher:used_users:";
    private static final String INITIAL_STOCK = "campaign:initial_stock:";
    private static final String INITIAL_QUOTA = "campaign:initial_voucher_quota:";
    private static final String PRODUCT_VOUCHER = "campaign:product_voucher:";
    private static final String NO_VOUCHER = "voucher:internal:none";

    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>(
            "local stock = redis.call('GET', KEYS[1]) \n"
            + "if not stock then return -3 end \n"
            + "if tonumber(stock) < tonumber(ARGV[1]) then return 0 end \n"
            + "if ARGV[3] == '0' then redis.call('DECRBY', KEYS[1], ARGV[1]); return 2 end \n"
            + "local quota = redis.call('GET', KEYS[2]) \n"
            + "if not quota then return -3 end \n"
            + "if tonumber(quota) <= 0 then return -1 end \n"
            + "if redis.call('SISMEMBER', KEYS[3], ARGV[2]) == 1 then return -2 end \n"
            + "redis.call('DECRBY', KEYS[1], ARGV[1]) \n"
            + "redis.call('DECRBY', KEYS[2], 1) \n"
            + "redis.call('SADD', KEYS[3], ARGV[2]) \n"
            + "return 1", Long.class);

    private final StringRedisTemplate redis;

    public RedisStockService(StringRedisTemplate redis) { this.redis = redis; }

    /** Returns a documented reservation status; all Redis mutations happen in one Lua execution. */
    public long reserve(Long productId, Long userId, int quantity, String voucherCode) {
        if (productId == null || userId == null || quantity <= 0) {
            throw new IllegalArgumentException("productId, userId, and quantity must be positive");
        }
        boolean hasVoucher = voucherCode != null && !voucherCode.isBlank();
        String code = hasVoucher ? voucherCode.trim() : NO_VOUCHER;
        Long result = redis.execute(RESERVE_SCRIPT,
                List.of(stockKey(productId), quotaKey(code), usedUsersKey(code)),
                Integer.toString(quantity), Long.toString(userId), hasVoucher ? "1" : "0");
        return Objects.requireNonNullElse(result, INVALID_RESOURCE);
    }

    public void initStock(Long productId, int stock) {
        redis.opsForValue().set(stockKey(productId), Integer.toString(stock));
        redis.opsForValue().set(initialStockKey(productId), Integer.toString(stock));
    }

    public void initializeCampaign(Long productId, int stock, String voucherCode, int voucherQuota) {
        initStock(productId, stock);
        redis.opsForValue().set(quotaKey(voucherCode), Integer.toString(voucherQuota));
        redis.opsForValue().set(initialQuotaKey(voucherCode), Integer.toString(voucherQuota));
        redis.opsForValue().set(productVoucherKey(productId), voucherCode);
        redis.delete(usedUsersKey(voucherCode));
    }

    public long currentStock(Long productId) { return readLong(stockKey(productId)); }
    public long initialStock(Long productId) { return readLong(initialStockKey(productId)); }
    public long currentVoucherQuota(String code) { return readLong(quotaKey(code)); }
    public long initialVoucherQuota(String code) { return readLong(initialQuotaKey(code)); }
    public String voucherCodeForProduct(Long productId) { return redis.opsForValue().get(productVoucherKey(productId)); }

    private long readLong(String key) {
        String value = redis.opsForValue().get(key);
        return value == null ? -1L : Long.parseLong(value);
    }
    private String stockKey(Long id) { return STOCK + id; }
    private String quotaKey(String code) { return QUOTA + code; }
    private String usedUsersKey(String code) { return USED + code; }
    private String initialStockKey(Long id) { return INITIAL_STOCK + id; }
    private String initialQuotaKey(String code) { return INITIAL_QUOTA + code; }
    private String productVoucherKey(Long id) { return PRODUCT_VOUCHER + id; }
}
