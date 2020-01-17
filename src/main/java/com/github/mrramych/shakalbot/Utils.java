package com.github.mrramych.shakalbot;

import com.github.mrramych.json.Json;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static com.github.mrramych.json.Json.object;
import static com.github.mrramych.json.Json.string;

public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    public static void configureLogging() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        LogManager.getLogManager().getLogger("").setLevel(Level.ALL);
        LOGGER.info("Configured JUL");

        LOGGER.info("Logging configured");
    }

    public static void response(OutputStream outputStream, int code, Json result) throws IOException {
        outputStream.write(object(
                "statusCode", code,
                "headers", object("Content-Type", "application/json"),
                "body", string(result.toString())
        ).toString().getBytes());
    }

    /**
     * If variable is not set, stops with error
     */
    public static String getAndCheckVariable(String name) {
        if (name == null) throw new NullPointerException();
        if (System.getenv(name) == null) {
            LOGGER.error("{} is not set", name);
            throw new NullPointerException("Variable '" + name + "' is not set");
        }
        return System.getenv(name);
    }

    public static Credential getGmailCredential() throws IOException {
        var cred = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setJsonFactory(JacksonFactory.getDefaultInstance())
                .setTransport(new NetHttpTransport())
                .setClientAuthentication(new ClientParametersAuthentication(
                        Utils.getAndCheckVariable("google_client_id"),
                        Utils.getAndCheckVariable("google_client_secret")
                ))
                .setTokenServerUrl(new GenericUrl("https://www.googleapis.com/oauth2/v4/token"))
                .build();
        cred.setFromTokenResponse(
                new TokenResponse()
                        .setAccessToken(getAndCheckVariable("google_token_access"))
                        .setRefreshToken(getAndCheckVariable("google_token_refresh"))
        );
        cred.refreshToken();
        LOGGER.info("Token refreshed");
        return cred;
    }
}
