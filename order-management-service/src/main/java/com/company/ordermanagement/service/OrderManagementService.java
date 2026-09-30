package com.company.ordermanagement.service;

import com.company.ordermanagement.dto.CreateOrderRequest;
import com.company.ordermanagement.dto.CreateOrderResponse;
import com.company.ordermanagement.dto.GetOrderTotalResponse;
import com.company.ordermanagement.dto.ListOrdersResponse;
import com.company.ordermanagement.dto.UpdateOrderStatusRequest;
import com.company.ordermanagement.dto.UpdateOrderStatusResponse;

public interface OrderManagementService {
  /** Creates a new order record. */
  CreateOrderResponse createOrder(CreateOrderRequest request);

  /** Updates the status for an existing order. */
  UpdateOrderStatusResponse updateOrderStatus(UpdateOrderStatusRequest request);

  /** Returns the total amount for a given order. */
  GetOrderTotalResponse getOrderTotal(Long orderId);

  /** Lists all orders for a customer. */
  ListOrdersResponse listOrders(Long customerId);
}
