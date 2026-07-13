package com.ecomera.order.order.controller;

import com.ecomera.order.order.dto.*;
import com.ecomera.order.order.enums.OrderStatus;
import com.ecomera.order.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private final UUID userId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final String email = "test@example.com";
    private final String rolesHeader = "USER,ADMIN";

    @Test
    void create_shouldReturn201() throws Exception {
        OrderCreateDto request = OrderCreateDto.builder()
                .userId(userId)
                .items(List.of(OrderItemCreateDto.builder()
                        .productId(UUID.randomUUID()).quantity(2).build()))
                .build();
        OrderDto orderDto = OrderDto.builder().id(orderId).userId(userId)
                .status(OrderStatus.PENDING).totalPrice(BigDecimal.TEN).orderItems(List.of()).build();
        given(orderService.create(any(UUID.class), anyString(), any(OrderCreateDto.class)))
                .willReturn(orderDto);

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Email", email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()));
    }

    @Test
    void create_shouldReturn400_whenInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Email", email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_shouldReturn201() throws Exception {
        OrderDto orderDto = OrderDto.builder().id(orderId).userId(userId)
                .status(OrderStatus.PENDING).totalPrice(BigDecimal.TEN).orderItems(List.of()).build();
        given(orderService.checkout(any(UUID.class), anyString())).willReturn(orderDto);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Email", email))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()));
    }

    @Test
    void updateStatus_shouldReturn200_whenAdmin() throws Exception {
        OrderUpdateDto request = OrderUpdateDto.builder().status(OrderStatus.CONFIRMED).build();
        OrderDto orderDto = OrderDto.builder().id(orderId).userId(userId)
                .status(OrderStatus.CONFIRMED).totalPrice(BigDecimal.TEN).orderItems(List.of()).build();
        given(orderService.updateStatus(any(UUID.class), any(OrderUpdateDto.class)))
                .willReturn(orderDto);

        mockMvc.perform(patch("/api/v1/orders/{id}", orderId)
                        .header("X-User-Roles", rolesHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_shouldReturn403_whenNotAdmin() throws Exception {
        OrderUpdateDto request = OrderUpdateDto.builder().status(OrderStatus.CONFIRMED).build();

        mockMvc.perform(patch("/api/v1/orders/{id}", orderId)
                        .header("X-User-Roles", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_shouldReturn200_whenOwnOrder() throws Exception {
        OrderDto orderDto = OrderDto.builder().id(orderId).userId(userId)
                .status(OrderStatus.PENDING).totalPrice(BigDecimal.TEN).orderItems(List.of()).build();
        given(orderService.getById(orderId)).willReturn(orderDto);

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isOk());
    }

    @Test
    void getById_shouldReturn403_whenNotOwnOrderAndNotAdmin() throws Exception {
        UUID otherUserId = UUID.randomUUID();
        OrderDto orderDto = OrderDto.builder().id(orderId).userId(otherUserId)
                .status(OrderStatus.PENDING).totalPrice(BigDecimal.TEN).orderItems(List.of()).build();
        given(orderService.getById(orderId)).willReturn(orderDto);

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_shouldReturn200_whenAdmin() throws Exception {
        Page<OrderDto> page = new PageImpl<>(List.of());
        given(orderService.getAll(any(PageRequest.class))).willReturn(page);

        mockMvc.perform(get("/api/v1/orders")
                        .header("X-User-Roles", rolesHeader))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByUser_shouldReturn200_whenOwnOrders() throws Exception {
        Page<OrderDto> page = new PageImpl<>(List.of());
        given(orderService.getByUserId(eq(userId), any(PageRequest.class))).willReturn(page);

        mockMvc.perform(get("/api/v1/orders/user/{userId}", userId)
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isOk());
    }

    @Test
    void getByUser_shouldReturn403_whenOtherUserAndNotAdmin() throws Exception {
        UUID otherUserId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/orders/user/{userId}", otherUserId)
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByStatus_shouldReturn200_whenAdmin() throws Exception {
        Page<OrderDto> page = new PageImpl<>(List.of());
        given(orderService.getByStatus(anyString(), any(PageRequest.class))).willReturn(page);

        mockMvc.perform(get("/api/v1/orders/status")
                        .param("status", "PENDING")
                        .header("X-User-Roles", rolesHeader))
                .andExpect(status().isOk());
    }

    @Test
    void getByStatus_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/orders/status")
                        .param("status", "PENDING")
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_shouldReturn204_whenAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/orders/{id}", orderId)
                        .header("X-User-Roles", rolesHeader))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/orders/{id}", orderId)
                        .header("X-User-Roles", "MANAGER"))
                .andExpect(status().isForbidden());
    }
}
