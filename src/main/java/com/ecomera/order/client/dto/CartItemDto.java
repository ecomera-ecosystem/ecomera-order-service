package com.ecomera.order.client.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record CartItemDto(
        UUID id,
        UUID productId,
        String productTitle,
        BigDecimal unitPrice,
        Integer quantity
) {
}
