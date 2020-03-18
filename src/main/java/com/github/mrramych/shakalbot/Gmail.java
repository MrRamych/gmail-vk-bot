package com.github.mrramych.shakalbot;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail.Builder;
import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.WatchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.LinkedList;
import java.util.List;

public class Gmail {

    public static final Logger LOGGER = LoggerFactory.getLogger(Gmail.class);

    final com.google.api.services.gmail.Gmail gmail;

    public Gmail() throws GeneralSecurityException, IOException {
        gmail = new Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), Utils.getGmailCredential())
                .setApplicationName("OAuth 2.0 Playground")
                .build();
    }

    /**
     * Subcribe to topic
     *
     * @return new historyId
     * @throws IOException
     */
    public BigInteger subscribe(String topic) throws IOException {
        var response = gmail.users()
                .watch("me", new WatchRequest().setTopicName(topic))
                .execute();

        return response.getHistoryId();
    }

    public void unsubscribe() throws IOException {
        LOGGER.info("Unsubscribing");
        gmail.users().stop("me").execute();
    }

    public Message prepareMessage(BigInteger startHistoryId) throws IOException, GeneralSecurityException {
        var response = gmail.users().history().list("me")
                .setStartHistoryId(startHistoryId)
                .setHistoryTypes(List.of("labelAdded", "labelRemoved", "messageAdded", "messageDeleted"))
                .execute();


        if (response.getHistory() == null) {
            LOGGER.warn("Got null");
            return null;
        }


        LinkedList<String> messages = new LinkedList<>();
        List<History> histories = response.getHistory();

        histories.forEach(history -> {
            StringBuilder builder = new StringBuilder();
            if (history.getLabelsAdded() != null) {
                //report about deleted messages
                history.getLabelsAdded().forEach(historyLabelAdded -> {
                    if (historyLabelAdded.getLabelIds().contains("TRASH")) {
                        try {
                            builder.append("Message deleted:\n");
                            writeInfoAboutMessage(builder, historyLabelAdded.getMessage().getId());
                        } catch (IOException e) {
                            LOGGER.warn("Can not get info about message", e);
                        }
                    }
                });
            }

            //report about new messages
            if (history.getMessagesAdded() != null) {
                history.getMessagesAdded().forEach(historyMessageAdded -> {
                    try {
                        if (historyMessageAdded.getMessage().getLabelIds() != null) {
                            if (historyMessageAdded.getMessage().getLabelIds().contains("INBOX")) {
                                builder.append("New incoming message:\n");
                            } else if (historyMessageAdded.getMessage().getLabelIds().contains("SPAM")) {
                                builder.append("New message in spam:\n");
                            } else if (historyMessageAdded.getMessage().getLabelIds().contains("SENT")) {
                                builder.append("New message sent:\n");
                            } else {
                                builder.append("New message detected somewhere:\n");
                            }
                            writeInfoAboutMessage(builder, historyMessageAdded.getMessage().getId());
                        }
                    } catch (IOException e) {
                        LOGGER.warn("Can get message info", e);
                    }
                });
            }

            if (builder.length() > 0) {
                messages.add(builder.toString());
            }
        });

        response.getHistory().forEach(history1 -> {
            LOGGER.info("Single history: {}", history1);
        });

        return new Message(messages, response.getHistoryId());
    }

    private void writeInfoAboutMessage(StringBuilder builder, String messageId) throws IOException {
        var messageInfo = gmail.users().messages().get("me", messageId)
                .execute();

        String from = messageInfo.getPayload().getHeaders().stream()
                .filter(messagePartHeader -> messagePartHeader.getName().equals("From"))
                .findFirst()
                .map(MessagePartHeader::getValue)
                .orElseGet(() -> "UNKNOWN");

        String subject = messageInfo.getPayload().getHeaders().stream()
                .filter(messagePartHeader -> messagePartHeader.getName().equals("Subject"))
                .findFirst()
                .map(MessagePartHeader::getValue)
                .orElseGet(() -> "NO SUBJECT");

        builder
                .append("From: ").append(from).append('\n')
                .append("Subject: ").append(subject).append('\n')
                .append("Snippet: ").append(messageInfo.getSnippet()).append("\n\n");

    }

    public static class Message {
        public final List<String> messages;
        public final BigInteger newHistoryId;

        public Message(List<String> messages, BigInteger newHistoryId) {
            this.messages = messages;
            this.newHistoryId = newHistoryId;
        }
    }

}
