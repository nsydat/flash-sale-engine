package com.fse.flashsale.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Redis is the inventory gate for the hot order path. The Lua script executes
 * its read-and-decrement sequence atomically on the Redis server.
 */
@Service
public class RedisStockService {

    private static final String STOCK_KEY_PREFIX = "product:stock:";

    private static final DefaultRedisScript<Long> DEDUCT_STOCK_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('GET', KEYS[1]) \n"
                    + "if not current then return -1 end \n"
                    + "if tonumber(current) < tonumber(ARGV[1]) then return 0 end \n"
                    + "redis.call('DECRBY', KEYS[1], ARGV[1]) \n"
                    + "return 1",
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    public RedisStockService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * @return 1 when stock was reserved, 0 when insufficient, or -1 when the stock key is absent.
     */
    public long deductStock(Long productId, int quantity) {
        if (productId == null || quantity <= 0) {
            throw new IllegalArgumentException("productId and quantity must be positive");
        }

        Long result = stringRedisTemplate.execute(
                DEDUCT_STOCK_SCRIPT,
                List.of(stockKey(productId)),
                Integer.toString(quantity)
        );
        return Objects.requireNonNullElse(result, -1L);
    }

    /** Replaces the Redis inventory value for a product during stock initialization. */
    public void initStock(Long productId, int stock) {
        if (productId == null || stock < 0) {
            throw new IllegalArgumentException("productId must be positive and stock cannot be negative");
        }
        stringRedisTemplate.opsForValue().set(stockKey(productId), Integer.toString(stock));
    }

    private String stockKey(Long productId) {
        return STOCK_KEY_PREFIX + productId;
    }
}
