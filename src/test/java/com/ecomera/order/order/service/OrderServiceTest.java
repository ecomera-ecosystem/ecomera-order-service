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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private CartServiceClient cartServiceClient;

    @Mock
    private NotificationEventProducer notificationProducer;

    @InjectMocks
    private OrderService orderService;

    private UUID userId;
    private UUID orderId;
    private UUID productId;
    private String email;
    private ProductDto productDto;
    private Order order;
    private OrderDto orderDto;
    private OrderItemCreateDto itemCreateDto;
    private OrderCreateDto orderCreateDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        productId = UUID.randomUUID();
        email = "test@example.com";

        productDto = ProductDto.builder()
                .id(productId)
                .title("Test Product")
                .price(BigDecimal.valueOf(29.99))
                .stock(10)
                .build();

        itemCreateDto = OrderItemCreateDto.builder()
                .productId(productId)
                .quantity(2)
                .build();

        orderCreateDto = OrderCreateDto.builder()
                .userId(userId)
                .items(List.of(itemCreateDto))
                .build();

        order = Order.builder()
                .id(orderId)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(59.98))
                .orderItems(new java.util.ArrayList<>())
                .build();

        orderDto = OrderDto.builder()
                .id(orderId)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(59.98))
                .orderItems(List.of())
                .build();
    }

    @Test
    void create_shouldCreateOrder() {
        given(orderMapper.toEntity(orderCreateDto)).willReturn(Order.builder().build());
        given(productServiceClient.getProductById(productId)).willReturn(productDto);
        given(orderRepository.save(any(Order.class))).willReturn(order);
        given(orderMapper.toDto(order)).willReturn(orderDto);

        OrderDto result = orderService.create(userId, email, orderCreateDto);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        verify(notificationProducer).sendNotification(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void create_shouldThrow_whenItemsEmpty() {
        OrderCreateDto emptyDto = OrderCreateDto.builder().userId(userId).items(List.of()).build();

        assertThatThrownBy(() -> orderService.create(userId, email, emptyDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empty items");
    }

    @Test
    void create_shouldThrow_whenStockInsufficient() {
        ProductDto lowStockProduct = ProductDto.builder()
                .id(productId).title("Test").price(BigDecimal.TEN).stock(1).build();
        given(orderMapper.toEntity(orderCreateDto)).willReturn(Order.builder().build());
        given(productServiceClient.getProductById(productId)).willReturn(lowStockProduct);

        assertThatThrownBy(() -> orderService.create(userId, email, orderCreateDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void checkout_shouldCreateOrderFromCart() {
        CartItemDto cartItemDto = CartItemDto.builder()
                .productId(productId).productTitle("Test")
                .unitPrice(BigDecimal.valueOf(29.99)).quantity(2).build();
        CartDto cartDto = CartDto.builder()
                .userId(userId).items(List.of(cartItemDto)).build();
        given(cartServiceClient.getCart(userId)).willReturn(cartDto);
        given(productServiceClient.getProductById(productId)).willReturn(productDto);
        given(orderRepository.save(any(Order.class))).willReturn(order);
        given(orderMapper.toDto(order)).willReturn(orderDto);

        OrderDto result = orderService.checkout(userId, email);

        assertThat(result).isNotNull();
        verify(cartServiceClient).clearCart(userId);
        verify(notificationProducer).sendNotification(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void checkout_shouldThrow_whenCartEmpty() {
        CartDto emptyCart = CartDto.builder().userId(userId).items(List.of()).build();
        given(cartServiceClient.getCart(userId)).willReturn(emptyCart);

        assertThatThrownBy(() -> orderService.checkout(userId, email))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empty cart");
    }

    @Test
    void checkout_shouldThrow_whenCartIsNull() {
        given(cartServiceClient.getCart(userId)).willReturn(null);

        assertThatThrownBy(() -> orderService.checkout(userId, email))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empty cart");
    }

    @Test
    void checkout_shouldThrow_whenStockInsufficient() {
        CartItemDto cartItemDto = CartItemDto.builder()
                .productId(productId).productTitle("Test")
                .unitPrice(BigDecimal.valueOf(29.99)).quantity(10).build();
        CartDto cartDto = CartDto.builder()
                .userId(userId).items(List.of(cartItemDto)).build();
        ProductDto lowStock = ProductDto.builder()
                .id(productId).title("Test").price(BigDecimal.TEN).stock(5).build();
        given(cartServiceClient.getCart(userId)).willReturn(cartDto);
        given(productServiceClient.getProductById(productId)).willReturn(lowStock);

        assertThatThrownBy(() -> orderService.checkout(userId, email))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void updateStatus_shouldUpdateStatus() {
        OrderUpdateDto updateDto = OrderUpdateDto.builder().status(OrderStatus.CONFIRMED).build();
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(orderRepository.save(any(Order.class))).willReturn(order);
        given(orderMapper.toDto(order)).willReturn(orderDto);

        OrderDto result = orderService.updateStatus(orderId, updateDto);

        assertThat(result).isNotNull();
        verify(notificationProducer).sendNotification(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void updateStatus_shouldThrow_whenNotFound() {
        OrderUpdateDto updateDto = OrderUpdateDto.builder().status(OrderStatus.CONFIRMED).build();
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateStatus(orderId, updateDto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_shouldReturnOrder() {
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(orderMapper.toDto(order)).willReturn(orderDto);

        OrderDto result = orderService.getById(orderId);

        assertThat(result).isEqualTo(orderDto);
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAll_shouldReturnPagedOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        given(orderRepository.findAll(pageable)).willReturn(orderPage);
        given(orderMapper.toDto(order)).willReturn(orderDto);

        Page<OrderDto> result = orderService.getAll(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getByUserId_shouldReturnPagedOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        given(orderRepository.findByUserId(userId, pageable)).willReturn(orderPage);
        given(orderMapper.toDto(order)).willReturn(orderDto);

        Page<OrderDto> result = orderService.getByUserId(userId, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getByStatus_shouldReturnOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        given(orderMapper.mapStatus("PENDING")).willReturn(OrderStatus.PENDING);
        given(orderRepository.findByStatus(OrderStatus.PENDING, pageable)).willReturn(orderPage);
        given(orderMapper.toDto(order)).willReturn(orderDto);

        Page<OrderDto> result = orderService.getByStatus("PENDING", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getByStatus_shouldThrow_whenInvalidStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        given(orderMapper.mapStatus("INVALID")).willReturn(null);

        assertThatThrownBy(() -> orderService.getByStatus("INVALID", pageable))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid status");
    }

    @Test
    void deleteById_shouldDeleteOrder() {
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        orderService.deleteById(orderId);

        verify(orderRepository).delete(order);
    }

    @Test
    void deleteById_shouldThrow_whenNotFound() {
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.deleteById(orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
