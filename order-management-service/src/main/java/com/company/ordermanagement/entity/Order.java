package com.company.ordermanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
  @SequenceGenerator(name = "orders_seq", sequenceName = "orders_seq", allocationSize = 1)
  @Column(name = "order_id")
  private Long orderId;

  @Column(name = "customer_id", nullable = false)
  private Long customerId;

  @Column(name = "order_date", nullable = false)
  private LocalDate orderDate;

  @Column(name = "total_amount")
  private BigDecimal totalAmount;

  @Column(name = "status")
  private String status;
}
