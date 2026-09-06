package com.grangloria.inventory.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("inventory_items")
public class InventoryItem {
    @Id
    private String itemName;
    private String description;
    private Integer quantity;
}