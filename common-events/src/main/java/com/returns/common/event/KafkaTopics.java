package com.returns.common.event;

public final class KafkaTopics {

    public static final String RETURN_INITIATED     = "return.initiated";
    public static final String RETURN_LABEL_CREATED   = "return.label_created";
    public static final String PACKAGE_RECEIVED      = "package.received";
    public static final String INVENTORY_RESTOCKED   = "inventory.restocked";
    public static final String REFUND_COMPLETED     = "refund.completed";

    private KafkaTopics() {
        // Private constructor prevents instantiation
    }
}