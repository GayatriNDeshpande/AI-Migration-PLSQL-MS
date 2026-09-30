package com.company.ordermanagement.exception;

public class OrderNotFoundException extends DomainException {
  private final String errorCode;

  public OrderNotFoundException(String message) {
    super(message);
    this.errorCode = "-20001";
  }

  public String getErrorCode() {
    return errorCode;
  }
}
