package com.ecomera.order.order.repository;

import com.ecomera.order.order.entity.Order;
import com.ecomera.order.order.entity.OrderItem;
import com.ecomera.order.order.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID userId;
    private Order order;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(59.98))
                .build();
        order = entityManager.persistAndFlush(order);
    }

    @Test
    void findByUserId_shouldReturnOrders() {
        Page<Order> result = orderRepository.findByUserId(userId, PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(userId);
    }

    @Test
    void findByUserId_shouldReturnEmpty_whenNoOrders() {
        Page<Order> result = orderRepository.findByUserId(UUID.randomUUID(), PageRequest.of(0, 10));
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findByStatus_shouldReturnOrders() {
        Order confirmedOrder = Order.builder()
                .userId(UUID.randomUUID())
                .status(OrderStatus.CONFIRMED)
                .totalPrice(BigDecimal.valueOf(19.99))
                .build();
        entityManager.persistAndFlush(confirmedOrder);

        Page<Order> result = orderRepository.findByStatus(OrderStatus.PENDING, PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void findByStatus_shouldReturnEmpty_whenNoMatch() {
        Page<Order> result = orderRepository.findByStatus(OrderStatus.CANCELED, PageRequest.of(0, 10));
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findById_shouldReturnOrder() {
        var found = orderRepository.findById(order.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(59.98));
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        var found = orderRepository.findById(UUID.randomUUID());
        assertThat(found).isEmpty();
    }

    @Test
    void save_shouldPersistOrderWithItems() {
        OrderItem item = OrderItem.builder()
                .productId(UUID.randomUUID())
                .productTitle("Test Product")
                .unitPrice(BigDecimal.valueOf(29.99))
                .quantity(2)
                .order(order)
                .build();
        order.getOrderItems().add(item);
        order.recalculateTotal();
        entityManager.persistAndFlush(item);

        Order saved = entityManager.find(Order.class, order.getId());
        assertThat(saved.getOrderItems()).hasSize(1);
        assertThat(saved.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(59.98));
    }

    @Test
    void findAll_shouldReturnAllOrders() {
        Page<Order> result = orderRepository.findAll(PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(1);
    }
}
