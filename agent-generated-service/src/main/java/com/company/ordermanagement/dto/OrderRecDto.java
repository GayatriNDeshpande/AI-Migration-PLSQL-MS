package com.company.ordermanagement.dto;

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
public class OrderRecDto {
  private Long orderId;
  private Long customerId;
  private LocalDate orderDate;
  private BigDecimal totalAmount;
}
