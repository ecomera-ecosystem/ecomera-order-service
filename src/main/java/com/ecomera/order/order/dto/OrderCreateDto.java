package com.ecomera.order.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
@Schema(name = "OrderCreateDto", description = "Payload for creating a new order")
public record OrderCreateDto(
        @NotNull
        UUID userId,

        @NotEmpty
        List<OrderItemCreateDto> items
) {
}
