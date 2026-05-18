package com.flowpay.FlowPay.mapper;

import com.flowpay.FlowPay.dto.OrderItemResponse;
import com.flowpay.FlowPay.dto.OrderResponse;
import com.flowpay.FlowPay.entity.Order;
import com.flowpay.FlowPay.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting {@link Order} and {@link OrderItem} entities
 * to their DTO counterparts.
 *
 * <p>Using {@code componentModel = "spring"} makes MapStruct generate a Spring
 * bean so the mapper can be injected with {@code @Autowired} / constructor
 * injection anywhere in the application.</p>
 *
 * <p>The {@link OrderItem#getOrder()} back-reference is intentionally excluded
 * from {@link OrderItemResponse} to break the circular serialization chain that
 * previously caused infinite JSON recursion.</p>
 */
@Mapper(componentModel = "spring")
public interface OrderMapper {

    /**
     * Maps a single {@link Order} entity to an {@link OrderResponse} DTO.
     * The nested {@code items} list is mapped via {@link #toOrderItemResponse(OrderItem)}.
     */
    OrderResponse toOrderResponse(Order order);

    /**
     * Maps a single {@link OrderItem} entity to an {@link OrderItemResponse} DTO.
     * The {@code order} back-reference field is not present in the target, so
     * MapStruct automatically ignores it.
     */
    OrderItemResponse toOrderItemResponse(OrderItem orderItem);

    /**
     * Convenience method for mapping a list of orders (used by the GET /api/orders endpoint).
     */
    List<OrderResponse> toOrderResponseList(List<Order> orders);
}
