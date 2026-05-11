package com.ecomera.order.client;

import com.ecomera.order.client.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "ecomera-product-service", path = "/api/v1/products")
public interface ProductServiceClient {

    @GetMapping("/{id}")
    ProductDto getProductById(@PathVariable("id") UUID id);
}
