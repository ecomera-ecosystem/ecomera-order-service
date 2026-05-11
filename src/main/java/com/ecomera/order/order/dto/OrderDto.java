package com.ecomera.order.order.dto;

import com.ecomera.order.order.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@Schema(name = "OrderDto", description = "Represents an order with items and metadata")
public record OrderDto(
        UUID id,
        UUID userId,
        OrderStatus status,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        List<OrderItemDto> orderItems
) {
}
