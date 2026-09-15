package com.grangloria.returns.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReturnRequest(
        @NotBlank(message = "Order ID is required")
        String orderId,

        @NotBlank(message = "SKU is required for inventory matching")
        String sku,

        @NotBlank(message = "Item description is required")
        String item,

        @NotBlank(message = "Customer email is required")
        @Email(message = "Invalid email format")
        String customerEmail,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity,

        @NotBlank(message = "ZIP code is required")
        String zipCode,

        @NotBlank(message = "Reason is required")
        String reason
) {}