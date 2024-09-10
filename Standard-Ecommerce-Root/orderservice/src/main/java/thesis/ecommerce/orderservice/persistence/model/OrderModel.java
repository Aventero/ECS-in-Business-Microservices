package thesis.ecommerce.orderservice.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import thesis.ecommerce.orderservice.api.dto.order.OrderStatus;

@Data
@Entity
@Table(name = "orders")
public class OrderModel {

    @Id
    private String id;

    private UUID cartId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private Instant orderDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    // Customer Info
    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String billingAddress;

    // Payment Info
    @Column(nullable = false)
    private String paymentMethod;

    @Column(nullable = false)
    private String paymentDetails;

    // Shipping Info
    @Column(nullable = false)
    private String shippingAddress;

    @Column(nullable = false)
    private String shippingMethod;

}