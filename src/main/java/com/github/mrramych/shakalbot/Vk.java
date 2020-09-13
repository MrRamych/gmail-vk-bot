package com.github.mrramych.shakalbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.Random;

import static com.github.mrramych.shakalbot.Utils.getAndCheckVariable;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.nio.charset.StandardCharsets.UTF_8;

public class Vk {

    private static final Logger LOGGER = LoggerFactory.getLogger(Vk.class);

    public void updateHistory(BigInteger newHistoryId) throws GeneralSecurityException, IOException, VkSendMessageException {
        DynamoDB db = new DynamoDB();
        var oldHistoryId = db.getIntValue("key", "previousHistoryId", "value");

        if (newHistoryId.compareTo(oldHistoryId) <= 0) {
            LOGGER.info("New historyId is lower or equal to old historyId. Ignoring. New={} Old={}", newHistoryId, oldHistoryId);
            return;
        }

        var message = new Gmail().prepareMessage(oldHistoryId);

        newHistoryId = message.newHistoryId;
        LOGGER.info("Sending {} messages", message.messages.size());
        message.messages.forEach(s -> {
            try {
                sendMessage(s);
            } catch (VkSendMessageException e) {
                LOGGER.warn("Can not send message", e);
            }
        });

        db.putIntValue("key", "previousHistoryId", "value", newHistoryId.toString());
    }

    private void sendMessage(String message) throws VkSendMessageException {
        try {
            LOGGER.info("Sending message {}", message.replace('\n', '\r'));

            String request = "https://api.vk.com/method/messages.send" +
                    "?access_token=" + getAndCheckVariable("vk_token") +
                    "&v=5.103" +
                    "&random_id=" + new Random().nextInt() +
                    "&chat_id=" + getAndCheckVariable("vk_target") +
                    "&message=" + URLEncoder.encode(message, UTF_8);

            LOGGER.info("Request is: " + request);
            HttpURLConnection con = (HttpURLConnection) new URL(request).openConnection();

            if (con.getResponseCode() == HTTP_OK) {
                LOGGER.info("VK response: " + new String(con.getInputStream().readAllBytes()));
            } else {
                LOGGER.warn("VK error: " + new String(con.getErrorStream().readAllBytes()));
                throw new VkSendMessageException("Can not send message " + new String(con.getErrorStream().readAllBytes()));
            }
        } catch (VkSendMessageException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Can not send message", e);
            throw new VkSendMessageException(e);
        }
    }

    public static class VkSendMessageException extends Exception {
        public VkSendMessageException(String message) {
            super(message);
        }

        public VkSendMessageException(Throwable cause) {
            super(cause);
        }
    }

}
