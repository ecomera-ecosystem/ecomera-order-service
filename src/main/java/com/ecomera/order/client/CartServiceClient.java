package com.ecomera.order.client;

import com.ecomera.order.client.dto.CartDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "ecomera-cart-service", path = "/api/v1/cart")
public interface CartServiceClient {

    @GetMapping
    CartDto getCart(@RequestHeader("X-User-Id") UUID userId);

    @DeleteMapping
    void clearCart(@RequestHeader("X-User-Id") UUID userId);
}
