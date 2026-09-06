package com.grangloria.inventory.dto;

public record ReturnCompletedEvent(
        String returnId,
        String orderId,
        String sku,
        int quantity,
        String warehouseLocationId
) {}