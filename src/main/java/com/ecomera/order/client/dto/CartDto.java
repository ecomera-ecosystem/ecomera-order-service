package com.ecomera.order.client.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record CartDto(
        UUID id,
        UUID userId,
        List<CartItemDto> items,
        BigDecimal totalPrice,
        Integer totalItems
) {
}
