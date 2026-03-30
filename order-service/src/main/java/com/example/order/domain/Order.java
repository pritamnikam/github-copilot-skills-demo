package com.example.order.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    public static Order of(String userId, String productId,
                           int quantity, double totalPrice) {
        Order o = new Order();
        o.userId     = userId;
        o.productId  = productId;
        o.quantity   = quantity;
        o.totalPrice = totalPrice;
        o.status     = OrderStatus.PENDING;
        o.createdAt  = Instant.now();
        return o;
    }

    // getters
    public String getId()          { return id; }
    public String getUserId()      { return userId; }
    public String getProductId()   { return productId; }
    public Integer getQuantity()   { return quantity; }
    public Double getTotalPrice()  { return totalPrice; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt()  { return createdAt; }
    public void setStatus(OrderStatus s) { this.status = s; }
}