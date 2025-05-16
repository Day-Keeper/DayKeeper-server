package com.shujinko.project.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;

@Component
public class GoogleTokenVerifier {

    @Value("${google.client-id.web}")
    private String clientIdWeb;

    @Value("${google.client-id.android}")
    private String clientIdAndroid;

    private static final HttpTransport transport = Utils.getDefaultTransport();
    private static final JsonFactory jsonFactory = Utils.getDefaultJsonFactory();

    public GoogleIdToken.Payload verify(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                    .setAudience(Arrays.asList(clientIdWeb, clientIdAndroid))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            return idToken != null ? idToken.getPayload() : null;
        } catch (Exception e) {
            return null;
        }
    }
}