package com.grangloria.inventory.entity;

import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.Map;

@Setter
@DynamoDbBean
public class InspectionAuditRecord {

    private String returnId;           // Partition Key
    private String itemSku;            // Sort Key
    private String binLocation;
    private String inspectorId;
    private String overallCondition;   // PASS, FAIL, REPAIR
    private Map<String, String> checklist; // Changed from Map<String, Object>
    private String inspectedAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("returnId")
    public String getReturnId() {
        return returnId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("itemSku")
    public String getItemSku() {
        return itemSku;
    }

    @DynamoDbAttribute("binLocation")
    public String getBinLocation() {
        return binLocation;
    }

    @DynamoDbAttribute("inspectorId")
    public String getInspectorId() {
        return inspectorId;
    }

    @DynamoDbAttribute("overallCondition")
    public String getOverallCondition() {
        return overallCondition;
    }

    @DynamoDbAttribute("checklist")
    public Map<String, String> getChecklist() {
        return checklist;
    }

    @DynamoDbAttribute("inspectedAt")
    public String getInspectedAt() {
        return inspectedAt;
    }
}