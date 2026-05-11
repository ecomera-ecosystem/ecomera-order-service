package com.ecomera.order.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Schema(name = "OrderItemDto", description = "Represents an order item")
public record OrderItemDto(
        UUID id,
        UUID productId,
        String productTitle,
        BigDecimal unitPrice,
        Integer quantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
