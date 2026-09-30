package com.company.ordermanagement.controller;

import com.company.ordermanagement.dto.CreateOrderRequest;
import com.company.ordermanagement.dto.CreateOrderResponse;
import com.company.ordermanagement.dto.GetOrderTotalResponse;
import com.company.ordermanagement.dto.ListOrdersResponse;
import com.company.ordermanagement.dto.UpdateOrderStatusRequest;
import com.company.ordermanagement.dto.UpdateOrderStatusResponse;
import com.company.ordermanagement.service.OrderManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order-management")
@RequiredArgsConstructor
@Validated
@Tag(name = "OrderManagement")
public class OrderManagementController {
  private final OrderManagementService service;

  @Operation(summary = "Create a new order")
  @ApiResponse(responseCode = "201", description = "Created", content = @Content)
  @PostMapping
  public ResponseEntity<CreateOrderResponse> createOrder(
      @Valid @RequestBody CreateOrderRequest request) {
    CreateOrderResponse response = service.createOrder(request);
    URI location = URI.create("/order-management/" + response.getOrderId());
    return ResponseEntity.created(location).body(response);
  }

  @Operation(summary = "Update an order status")
  @ApiResponse(responseCode = "200", description = "Updated", content = @Content)
  @PutMapping("/{orderId}/status")
  public ResponseEntity<UpdateOrderStatusResponse> updateOrderStatus(
      @PathVariable("orderId") @NotNull @Positive Long orderId,
      @Valid @RequestBody UpdateOrderStatusRequest request) {
    UpdateOrderStatusRequest updated = UpdateOrderStatusRequest.builder()
        .orderId(orderId)
        .status(request.getStatus())
        .build();
    return ResponseEntity.ok(service.updateOrderStatus(updated));
  }

  @Operation(summary = "Get order total")
  @ApiResponse(responseCode = "200", description = "OK", content = @Content)
  @GetMapping("/{orderId}/total")
  public ResponseEntity<GetOrderTotalResponse> getOrderTotal(
      @PathVariable("orderId") @NotNull @Positive Long orderId) {
    return ResponseEntity.ok(service.getOrderTotal(orderId));
  }

  @Operation(summary = "List orders for a customer")
  @ApiResponse(responseCode = "200", description = "OK", content = @Content)
  @GetMapping("/customer/{customerId}/orders")
  public ResponseEntity<ListOrdersResponse> listOrders(
      @PathVariable("customerId") @NotNull @Positive Long customerId) {
    return ResponseEntity.status(HttpStatus.OK).body(service.listOrders(customerId));
  }
}
