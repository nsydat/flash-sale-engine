package com.fse.flashsale.repository;

import com.fse.flashsale.entity.Order;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(String orderCode);

    @Query("select coalesce(sum(o.quantity), 0) from Order o where o.productId = :productId and o.status = 'SUCCESS'")
    Long totalSuccessfulQuantityByProductId(@Param("productId") Long productId);

    long countByProductIdAndVoucherCodeAndStatus(Long productId, String voucherCode, String status);

    long countByVoucherCodeAndStatus(String voucherCode, String status);

    boolean existsByUserIdAndVoucherCodeAndStatus(Long userId, String voucherCode, String status);

    @Query("select count(distinct o.userId) from Order o where o.voucherCode = :voucherCode and o.status = 'SUCCESS'")
    long countDistinctVoucherUsers(@Param("voucherCode") String voucherCode);
}
