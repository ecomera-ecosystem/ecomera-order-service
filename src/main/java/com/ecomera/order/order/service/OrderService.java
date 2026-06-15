package com.ecomera.order.order.service;

import com.ecomera.order.client.CartServiceClient;
import com.ecomera.order.client.ProductServiceClient;
import com.ecomera.order.client.dto.CartDto;
import com.ecomera.order.client.dto.CartItemDto;
import com.ecomera.order.client.dto.ProductDto;
import com.ecomera.order.order.dto.OrderCreateDto;
import com.ecomera.order.order.dto.OrderDto;
import com.ecomera.order.order.dto.OrderItemCreateDto;
import com.ecomera.order.order.dto.OrderUpdateDto;
import com.ecomera.order.order.entity.Order;
import com.ecomera.order.order.entity.OrderItem;
import com.ecomera.order.order.enums.OrderStatus;
import com.ecomera.order.order.mapper.OrderMapper;
import com.ecomera.order.order.repository.OrderRepository;
import com.ecomera.order.shared.common.exception.BusinessException;
import com.ecomera.order.shared.common.exception.ResourceNotFoundException;
import com.ecomera.order.shared.kafka.NotificationEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductServiceClient productServiceClient;
    private final CartServiceClient cartServiceClient;
    private final NotificationEventProducer notificationProducer;

    @Transactional
    public OrderDto create(UUID userId, String email, OrderCreateDto dto) {
        if (dto.items() == null || dto.items().isEmpty()) {
            throw new BusinessException("Cannot create order with empty items");
        }

        Order order = orderMapper.toEntity(dto);
        order.setUserId(userId);
        order.setOrderItems(new ArrayList<>());

        for (OrderItemCreateDto itemDto : dto.items()) {
            ProductDto product = productServiceClient.getProductById(itemDto.productId());
            validateStock(product, itemDto.quantity());

            OrderItem item = OrderItem.builder()
                    .productId(product.id())
                    .productTitle(product.title())
                    .unitPrice(product.price())
                    .quantity(itemDto.quantity())
                    .order(order)
                    .build();

            order.getOrderItems().add(item);
        }

        order.recalculateTotal();
        Order saved = orderRepository.save(order);
        log.info("Order created: {} for user: {}", saved.getId(), userId);

        notificationProducer.sendNotification(
                email,
                "Order Created",
                "Your order #" + saved.getId() + " has been created with status " + saved.getStatus() + ".",
                "EMAIL",
                "ecomera-order-service"
        );

        return orderMapper.toDto(saved);
    }

    @Transactional
    public OrderDto checkout(UUID userId, String email) {
        CartDto cart = cartServiceClient.getCart(userId);
        if (cart == null || cart.items() == null || cart.items().isEmpty()) {
            throw new BusinessException("Cannot checkout with an empty cart");
        }

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.ZERO)
                .build();

        for (CartItemDto cartItem : cart.items()) {
            ProductDto product = productServiceClient.getProductById(cartItem.productId());
            if (product.stock() < cartItem.quantity()) {
                throw new BusinessException(
                        "Insufficient stock for '" + product.title() + "'. Available: " + product.stock()
                                + ", In cart: " + cartItem.quantity()
                );
            }
            OrderItem orderItem = OrderItem.builder()
                    .productId(product.id())
                    .productTitle(product.title())
                    .unitPrice(cartItem.unitPrice())
                    .quantity(cartItem.quantity())
                    .order(order)
                    .build();
            order.addItem(orderItem);
        }

        Order saved = orderRepository.save(order);
        cartServiceClient.clearCart(userId);
        log.info("Checkout completed for user {}. Order id: {}", userId, saved.getId());

        notificationProducer.sendNotification(
                email,
                "Order Checkout",
                "Your order #" + saved.getId() + " has been placed successfully. Total: " + saved.getTotalPrice() + " MAD.",
                "EMAIL",
                "ecomera-order-service"
        );

        return orderMapper.toDto(saved);
    }

    @Transactional
    @Caching(
            put = @CachePut(value = "orders", key = "#id"),
            evict = @CacheEvict(value = "orders-summary", allEntries = true)
    )
    public OrderDto updateStatus(UUID id, OrderUpdateDto dto) {
        Order order = findOrderById(id);
        orderMapper.updateEntityFromDto(dto, order);
        Order saved = orderRepository.save(order);
        log.info("Order {} status updated to: {}", id, saved.getStatus());

        notificationProducer.sendNotification(
                saved.getUserId().toString(),
                "Order Status Updated",
                "Your order #" + saved.getId() + " is now " + saved.getStatus() + ".",
                "EMAIL",
                "ecomera-order-service"
        );

        return orderMapper.toDto(saved);
    }

    @Cacheable(value = "orders", key = "#id")
    public OrderDto getById(UUID id) {
        log.debug("Cache miss fetching order {} from DB", id);
        return orderRepository.findById(id)
                .map(orderMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Order.class, "id", id));
    }

    public Page<OrderDto> getAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(orderMapper::toDto);
    }

    public Page<OrderDto> getByUserId(UUID userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(orderMapper::toDto);
    }

    public Page<OrderDto> getByStatus(String status, Pageable pageable) {
        OrderStatus orderStatus = orderMapper.mapStatus(status);
        if (orderStatus == null) {
            throw new BusinessException("Invalid status: " + status);
        }
        return orderRepository.findByStatus(orderStatus, pageable).map(orderMapper::toDto);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", key = "#id"),
            @CacheEvict(value = "orders-summary", allEntries = true)
    })
    public void deleteById(UUID id) {
        Order order = findOrderById(id);
        orderRepository.delete(order);
        log.info("Order deleted: {}", id);
    }

    private Order findOrderById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Order.class, "id", id));
    }

    private void validateStock(ProductDto product, int quantity) {
        if (product.stock() < quantity) {
            throw new BusinessException(
                    "Insufficient stock for product '" + product.title()
                            + "'. Available: " + product.stock() + ", Requested: " + quantity
            );
        }
    }
}
