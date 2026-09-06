package com.grangloria.returns.event;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReturnRequest(
        @NotBlank(message = "SKU is required")
        String sku,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity,

        @NotBlank(message = "Pickup ZIP is required")
        @Pattern(regexp = "^\\d{5}$", message = "ZIP must be 5 digits")
        String pickupZip,

        @NotBlank(message = "Customer ID is required") // Added this
        String customerId,

        @Email(message = "Invalid email format")
        String customerEmail,

        String reasonCode,
        String orderId
) {}