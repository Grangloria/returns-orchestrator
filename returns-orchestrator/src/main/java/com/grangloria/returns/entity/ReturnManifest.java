package com.grangloria.returns.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("return_manifests")
public class ReturnManifest {
    @Id
    private UUID id;
    private String trackingId;
    private String sku;
    private Integer quantity;
    private String customerEmail;
    private String orderId;
    private String status;
    private LocalDateTime createdAt;
    private String labelUrl;
}