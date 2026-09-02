package com.fse.flashsale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/** Event published after Redis reserves the requested inventory. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String orderCode;
    private Long userId;
    private Long productId;
    private Integer quantity;
}
