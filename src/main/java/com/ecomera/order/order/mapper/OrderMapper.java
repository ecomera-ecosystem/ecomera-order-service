package com.ecomera.order.order.mapper;

import com.ecomera.order.order.dto.OrderCreateDto;
import com.ecomera.order.order.dto.OrderDto;
import com.ecomera.order.order.dto.OrderUpdateDto;
import com.ecomera.order.order.entity.Order;
import com.ecomera.order.order.enums.OrderStatus;
import com.ecomera.order.shared.common.mapper.BaseMappingConfig;
import org.mapstruct.*;

@Mapper(config = BaseMappingConfig.class, uses = OrderItemMapper.class)
public interface OrderMapper {

    @Mapping(target = "orderItems", source = "orderItems")
    @Mapping(target = "createdAt", source = "createdAt")
    OrderDto toDto(Order order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "userId", source = "userId")
    Order toEntity(OrderCreateDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(OrderUpdateDto dto, @MappingTarget Order order);

    default OrderStatus mapStatus(String status) {
        return status != null ? OrderStatus.valueOf(status.toUpperCase()) : null;
    }
}
