package com.fse.flashsale.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CampaignInitRequest(
        @NotNull @Positive Long productId, @NotNull @PositiveOrZero Integer stock,
        @NotBlank String voucherCode, @NotNull @PositiveOrZero Integer voucherQuota,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotNull @DecimalMin("0.00") BigDecimal discountAmount) { }
