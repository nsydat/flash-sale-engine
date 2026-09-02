package com.fse.flashsale.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Validated API payload for reserving flash-sale inventory. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleRequest {

    @NotNull
    @Positive
    private Long productId;

    @NotNull
    @Positive
    private Long userId;

    @NotNull
    @Positive
    private Integer quantity;
}
