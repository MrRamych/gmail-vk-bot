package com.github.mrramych.shakalbot;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;

import java.math.BigInteger;
import java.util.Map;

public class DynamoDB {

    private final DynamoDbClient client;

    public DynamoDB() {
        client = DynamoDbClient.builder().build();
    }

    /**
     * @param key   where to search
     * @param value value of key
     * @param what  what to return
     * @return
     */
    public BigInteger getIntValue(String key, String value, String what) {
        var item = client.getItem(builder -> builder
                .tableName("Shakal-Bot-DB")
                .key(Map.of(key, AttributeValue.builder().s(value).build()))
        );
        return new BigInteger(item.item().get("value").n());
    }

    public void putIntValue(String key, String value, String where, String what) {
        client.updateItem(builder -> builder
                .tableName("Shakal-Bot-DB")
                .key(Map.of(
                        key,
                        AttributeValue.builder().s(value).build()))
                .attributeUpdates(Map.of(
                        where,
                        AttributeValueUpdate.builder()
                                .value(AttributeValue.builder().n(what).build())
                                .action(AttributeAction.PUT)
                                .build()
                )));
    }
}
