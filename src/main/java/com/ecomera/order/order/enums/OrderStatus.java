package com.ecomera.order.order.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
@Schema(name = "OrderStatus", description = "Represents the lifecycle status of an order")
public enum OrderStatus {

    PENDING("Pending"),
    PROCESSING("Processing"),
    CONFIRMED("Confirmed"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELED("Canceled");

    @JsonValue
    private final String statusName;

    public static Optional<OrderStatus> fromString(String value) {
        return Arrays.stream(values())
                .filter(os -> os.name().equalsIgnoreCase(value) || os.getStatusName().equalsIgnoreCase(value))
                .findFirst();
    }

    @JsonCreator
    public static OrderStatus forValue(String value) {
        return fromString(value)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid status: " + value + ". Valid values are: " +
                                Arrays.stream(values()).map(OrderStatus::getStatusName).collect(Collectors.joining(", "))
                ));
    }
}
