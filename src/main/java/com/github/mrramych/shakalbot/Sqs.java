package com.github.mrramych.shakalbot;


import moe.orangelabs.json.Json;
import moe.orangelabs.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.io.IOException;
import java.util.Base64;

import static moe.orangelabs.json.Json.parse;


public class Sqs {

    private static final Logger LOGGER = LoggerFactory.getLogger(Sqs.class);

    private final SqsClient client;

    public Sqs() {
        client = SqsClient.builder().build();
    }

    public void sendToSqs(Json json) throws IOException {
        if (!json.isString()) {
            LOGGER.warn("Expected body to be string, but received '{}'", json);
            throw new IllegalArgumentException();
        }

        JsonObject input = parse(json.getAsString().string).getAsObject();

        JsonObject message = parse(new String(
                Base64.getDecoder().decode(input.getObject("message").getString("data").string)
        )).getAsObject();


        LOGGER.info("Your SQS message is: {}", message.toString());

        client.sendMessage(SendMessageRequest.builder()
                .queueUrl(Utils.getAndCheckVariable("sqs_url"))
                .messageBody(message.toString())
                .build());

        LOGGER.info("Done");
    }

}
