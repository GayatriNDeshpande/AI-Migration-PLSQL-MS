package com.company.ordermanagement.service;

import com.company.ordermanagement.dto.CreateOrderRequest;
import com.company.ordermanagement.dto.CreateOrderResponse;
import com.company.ordermanagement.dto.GetOrderTotalResponse;
import com.company.ordermanagement.dto.ListOrdersResponse;
import com.company.ordermanagement.dto.OrderRecDto;
import com.company.ordermanagement.dto.UpdateOrderStatusRequest;
import com.company.ordermanagement.dto.UpdateOrderStatusResponse;
import com.company.ordermanagement.entity.AuditLog;
import com.company.ordermanagement.entity.Order;
import com.company.ordermanagement.exception.OrderNotFoundException;
import com.company.ordermanagement.repository.AuditLogRepository;
import com.company.ordermanagement.repository.OrderRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderManagementServiceImpl implements OrderManagementService {
  private final OrderRepository orderRepository;
  private final AuditLogRepository auditLogRepository;

  @Override
  public CreateOrderResponse createOrder(CreateOrderRequest request) {
    Order order = Order.builder()
        .customerId(request.getCustomerId())
        .orderDate(request.getOrderDate())
        .totalAmount(request.getTotalAmount())
        .status("NEW")
        .build();

    Order saved = orderRepository.save(order);
    log.info("Created order {}", saved.getOrderId());
    logAudit("Created order " + saved.getOrderId());

    return CreateOrderResponse.builder()
        .orderId(saved.getOrderId())
        .build();
  }

  @Override
  public UpdateOrderStatusResponse updateOrderStatus(UpdateOrderStatusRequest request) {
    Order order = orderRepository.findByIdForUpdate(request.getOrderId())
        .orElseThrow(() -> new OrderNotFoundException("Order not found: " + request.getOrderId()));

    order.setStatus(request.getStatus());
    orderRepository.save(order);

    logAudit("Updated order " + request.getOrderId() + " to " + request.getStatus());

    return UpdateOrderStatusResponse.builder()
        .orderId(order.getOrderId())
        .status(order.getStatus())
        .build();
  }

  @Override
  @Transactional(Transactional.TxType.SUPPORTS)
  public GetOrderTotalResponse getOrderTotal(Long orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

    BigDecimal total = order.getTotalAmount() == null
        ? BigDecimal.ZERO
        : order.getTotalAmount();

    return GetOrderTotalResponse.builder()
        .orderId(order.getOrderId())
        .totalAmount(total)
        .build();
  }

  @Override
  @Transactional(Transactional.TxType.SUPPORTS)
  public ListOrdersResponse listOrders(Long customerId) {
    List<OrderRecDto> orders = orderRepository.findByCustomerId(customerId).stream()
        .map(order -> OrderRecDto.builder()
            .orderId(order.getOrderId())
            .customerId(order.getCustomerId())
            .orderDate(order.getOrderDate())
            .totalAmount(order.getTotalAmount())
            .build())
        .toList();

    return ListOrdersResponse.builder()
        .customerId(customerId)
        .orders(orders)
        .build();
  }

  @Transactional(Transactional.TxType.REQUIRES_NEW)
  protected void logAudit(String message) {
    try {
      AuditLog auditLog = AuditLog.builder()
          .message(message)
          .createdAt(Instant.now())
          .build();
      auditLogRepository.save(auditLog);
    } catch (Exception ex) {
      log.warn("Audit logging failed: {}", message, ex);
    }
  }
}
