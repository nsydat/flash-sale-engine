package com.fse.flashsale.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Validated API payload for loading a product's Redis inventory. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitStockRequest {

    @NotNull
    @Positive
    private Long productId;

    @NotNull
    @PositiveOrZero
    private Integer stock;
}
