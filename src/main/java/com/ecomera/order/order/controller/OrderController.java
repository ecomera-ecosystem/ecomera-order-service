package com.ecomera.order.order.controller;

import com.ecomera.order.order.dto.OrderCreateDto;
import com.ecomera.order.order.dto.OrderDto;
import com.ecomera.order.order.dto.OrderUpdateDto;
import com.ecomera.order.order.service.OrderService;
import com.ecomera.order.shared.common.exception.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management APIs")
public class OrderController {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String MANAGER_ROLE = "MANAGER";

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order")
    @ApiResponse(responseCode = "201", description = "Order created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid order data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<OrderDto> create(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody OrderCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(userId, dto));
    }

    @PostMapping("/checkout")
    @Operation(summary = "Checkout", description = "Convert current cart into an order")
    @ApiResponse(responseCode = "201", description = "Order created from cart")
    @ApiResponse(responseCode = "400", description = "Cart is empty or insufficient stock")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<OrderDto> checkout(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.checkout(userId));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update order status")
    @ApiResponse(responseCode = "200", description = "Order updated successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderDto> updateStatus(
            @RequestHeader("X-User-Roles") String roles,
            @Parameter(description = "Order UUID") @PathVariable UUID id,
            @Valid @RequestBody OrderUpdateDto dto) {
        requireAdminOrManager(roles);
        return ResponseEntity.ok(orderService.updateStatus(id, dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    @ApiResponse(responseCode = "200", description = "Order retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderDto> getById(
            @RequestHeader("X-User-Roles") String roles,
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Order UUID") @PathVariable UUID id) {
        OrderDto order = orderService.getById(id);
        if (!order.userId().equals(userId)) {
            requireAdminOrManager(roles);
        }
        return ResponseEntity.ok(order);
    }

    @GetMapping
    @Operation(summary = "Get all orders (paginated)")
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<Page<OrderDto>> getAll(
            @RequestHeader("X-User-Roles") String roles,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        requireAdminOrManager(roles);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(orderService.getAll(pageable));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get orders by user ID")
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<Page<OrderDto>> getByUser(
            @RequestHeader("X-User-Roles") String roles,
            @RequestHeader("X-User-Id") UUID currentUserId,
            @Parameter(description = "Target user UUID") @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!currentUserId.equals(userId)) {
            requireAdminOrManager(roles);
        }
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(orderService.getByUserId(userId, pageable));
    }

    @GetMapping("/status")
    @Operation(summary = "Get orders by status")
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid status")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<Page<OrderDto>> getByStatus(
            @RequestHeader("X-User-Roles") String roles,
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        requireAdminOrManager(roles);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(orderService.getByStatus(status, pageable));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete order")
    @ApiResponse(responseCode = "204", description = "Order deleted successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Roles") String roles,
            @Parameter(description = "Order UUID") @PathVariable UUID id) {
        requireAdminRole(roles);
        orderService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void requireAdminOrManager(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()
                || (!rolesHeader.contains(ADMIN_ROLE) && !rolesHeader.contains(MANAGER_ROLE))) {
            throw new ApiException("Insufficient permissions. Requires ADMIN or MANAGER role.", HttpStatus.FORBIDDEN);
        }
    }

    private void requireAdminRole(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()
                || !rolesHeader.contains(ADMIN_ROLE)) {
            throw new ApiException("Insufficient permissions. Requires ADMIN role.", HttpStatus.FORBIDDEN);
        }
    }
}
