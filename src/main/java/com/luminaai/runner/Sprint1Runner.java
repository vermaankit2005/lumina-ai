package com.luminaai.runner;

import com.luminaai.service.gmail.GmailFetchService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Sprint1Runner implements CommandLineRunner {

    private final GmailFetchService gmailFetchService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=========================================");
        System.out.println("🚀 Lumina AI - Sprint 1 Execution Start");
        System.out.println("=========================================");

        gmailFetchService.fetchRecentEmails();

        System.out.println("=========================================");
        System.out.println("✅ Sprint 1 Execution Complete");
        System.out.println("=========================================");
    }
}