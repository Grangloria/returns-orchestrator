package com.grangloria.inventory.repository;

import com.grangloria.inventory.entity.InspectionAuditRecord;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;

@Repository
public class InventoryAuditRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<InspectionAuditRecord> auditTable;

    public InventoryAuditRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    @PostConstruct
    public void init() {
        this.auditTable = enhancedClient.table("inventory_inspection_audit", TableSchema.fromBean(InspectionAuditRecord.class));
        // Auto-create table in local environment if it doesn't exist
        try {
            this.auditTable.createTable();
        } catch (Exception ignored) {
            // Table already exists
        }
    }

    public void save(InspectionAuditRecord record) {
        auditTable.putItem(record);
    }

    public Optional<InspectionAuditRecord> findByReturnIdAndSku(String returnId, String itemSku) {
        Key key = Key.builder()
                .partitionValue(returnId)
                .sortValue(itemSku)
                .build();
        return Optional.ofNullable(auditTable.getItem(key));
    }
}