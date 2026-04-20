package com.luminaai.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collections;
import java.util.List;

/**
 * Configures the Gmail API client using OAuth 2.0.
 *
 * <p>If the credentials file is absent (e.g. first-time setup or CI), the bean returns
 * {@code null} and {@link com.luminaai.service.gmail.GmailFetchService} degrades gracefully.
 */
@Slf4j
@Configuration
public class GmailApiConfig {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_READONLY);

    @Value("${lumina.gmail.credentials-file-path}")
    private String credentialsFilePath;

    @Value("${lumina.gmail.tokens-directory-path}")
    private String tokensDirectoryPath;

    @Bean
    public Gmail gmailService() throws Exception {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleClientSecrets clientSecrets;
        try {
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new FileReader(credentialsFilePath));
        } catch (FileNotFoundException e) {
            log.warn("Gmail credentials file not found at '{}'. Gmail fetch will be disabled.", credentialsFilePath);
            return null;
        }

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(tokensDirectoryPath)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName("Lumina AI")
                .build();
    }
}
