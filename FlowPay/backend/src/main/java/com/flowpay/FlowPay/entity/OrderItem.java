package com.flowpay.FlowPay.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String itemName;

    private Integer quantity;

    private Double unitPrice;

    private Double totalPrice;

    /**
     * {@code @JsonBackReference} marks this as the "child" side of the
     * Order ↔ OrderItem relationship. Jackson will NOT serialize this field,
     * breaking the infinite recursion cycle
     * ({@code Order → items → order → items → ...}).
     */
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
}
