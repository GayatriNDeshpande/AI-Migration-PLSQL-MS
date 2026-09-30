package com.company.ordermanagement.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetOrderTotalResponse {
  private Long orderId;
  private BigDecimal totalAmount;
}
