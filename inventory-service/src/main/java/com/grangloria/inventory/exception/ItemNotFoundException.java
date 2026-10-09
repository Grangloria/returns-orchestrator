package com.grangloria.inventory.exception;

import org.springframework.http.HttpStatus;

public class ItemNotFoundException extends InventoryException {

    public ItemNotFoundException(String message) {
        super(message, "ITEM_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}