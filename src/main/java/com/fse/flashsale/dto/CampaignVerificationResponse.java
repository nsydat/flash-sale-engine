package com.fse.flashsale.dto;
public record CampaignVerificationResponse(Long productId, String voucherCode, long databaseOrderQuantity,
        long redisStockDeductions, long databaseVoucherRedemptions, long redisVoucherQuotaDeductions,
        boolean zeroOverselling, boolean zeroDuplicateVoucherUsage, boolean isConsistent) { }
