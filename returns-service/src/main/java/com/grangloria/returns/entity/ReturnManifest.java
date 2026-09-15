package com.grangloria.returns.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("return_manifests")
public class ReturnManifest implements Persistable<String> {

    @Id
    @Column("order_id")
    private String orderId;

    private String sku;

    @Column("item_name")
    private String item;

    private Integer quantity;

    @Column("customer_email")
    private String customerEmail;

    @Column("zip_code")
    private String zipCode;

    private String reason;

    // Refactored from String to ReturnState Enum
    private ReturnState status;

    @Column("label_url")
    private String labelUrl;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Transient
    @Builder.Default
    private boolean isNewEntity = true;

    @Override
    public String getId() {
        return this.orderId;
    }

    @Override
    public boolean isNew() {
        return this.isNewEntity;
    }
}