package com.ecomera.order.order.dto;

import com.ecomera.order.order.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "OrderUpdateDto", description = "Payload for updating an existing order")
public record OrderUpdateDto(
        OrderStatus status
) {
}
