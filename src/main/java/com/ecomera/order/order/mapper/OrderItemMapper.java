package com.ecomera.order.order.mapper;

import com.ecomera.order.order.dto.OrderItemCreateDto;
import com.ecomera.order.order.dto.OrderItemDto;
import com.ecomera.order.order.entity.OrderItem;
import com.ecomera.order.shared.common.mapper.BaseMappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = BaseMappingConfig.class)
public interface OrderItemMapper {

    OrderItemDto toDto(OrderItem entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "productTitle", ignore = true)
    @Mapping(target = "productId", source = "productId")
    OrderItem toEntity(OrderItemCreateDto dto);
}
