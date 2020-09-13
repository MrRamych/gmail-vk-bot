package com.github.mrramych.shakalbot;


import com.amazonaws.services.lambda.runtime.Context;
import moe.orangelabs.json.JsonObject;
import moe.orangelabs.json.exceptions.JsonCastException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Scanner;

import static com.github.mrramych.shakalbot.Utils.*;
import static java.net.HttpURLConnection.HTTP_OK;
import static moe.orangelabs.json.Json.parse;
import static moe.orangelabs.json.Json.string;

public class Handler {

    private static final Logger LOGGER;

    static {
        LOGGER = LoggerFactory.getLogger(Handler.class);
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        System.out.println("Waiting for input");

        new Handler().handle(
                new ByteArrayInputStream(new Scanner(System.in).nextLine().getBytes()),
                new ByteArrayOutputStream(),
                null);
    }

    public void handle(InputStream inputStream, OutputStream outputStream, Context context) throws IOException, GeneralSecurityException {
        System.setProperty("line.separator", "\r");

        configureLogging();

        String in = new String(inputStream.readAllBytes());
        LOGGER.info("Input: " + in);

        if (in.equals("{\"unsubscribe\"")) {
            LOGGER.info("Unsubscribing");
            new Gmail().unsubscribe();
            return;
        }

        JsonObject input;
        try {
            input = parse(in).getAsObject();
        } catch (JsonCastException e) {
            LOGGER.warn("Can not parse message", e);
            response(outputStream, HTTP_OK, string("ok"));
            return;
        }


        if (input.containsKey("Records")) {
            LOGGER.info("Updating history");

            var body = input.get("Records").getAsArray().get(0).getAsObject().getString("body").string;
            var message = parse(body).getAsObject();
            var newHistoryId = message.getNumber("historyId").value;

            try {
                new Vk().updateHistory(newHistoryId.toBigInteger());
            } catch (Vk.VkSendMessageException e) {
                LOGGER.warn("Can not send message", e);
            }

            response(outputStream, HTTP_OK, string("ok"));
        } else if (input.containsKey("path")) {
            LOGGER.info("Sending message to Sqs");

            try {
                new Sqs().sendToSqs(input.get("body"));
            } catch (Exception e) {
                LOGGER.warn("Can not send message to SQS", e);
            }

            response(outputStream, HTTP_OK, string("ok"));
        } else {
            LOGGER.info("Subscribing");

            try {
                var newHistoryId = new Gmail().subscribe(getAndCheckVariable("google_topic"));

                new Vk().updateHistory(newHistoryId);
            } catch (Exception e) {
                LOGGER.warn("Can not send message ", e);
            }

            response(outputStream, HTTP_OK, string("ok"));
        }
    }

}
