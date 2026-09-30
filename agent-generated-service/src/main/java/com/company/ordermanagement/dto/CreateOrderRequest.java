package com.company.ordermanagement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class CreateOrderRequest {
  @NotNull
  @Positive
  private Long customerId;

  @NotNull
  private LocalDate orderDate;

  @NotNull
  @Positive
  private BigDecimal totalAmount;
}
