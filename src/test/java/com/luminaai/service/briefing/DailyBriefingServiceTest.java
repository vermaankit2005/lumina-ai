package com.luminaai.service.briefing;

import com.luminaai.domain.model.AnalysisResult;
import com.luminaai.domain.model.EmailMessage;
import com.luminaai.entity.BriefingRun;
import com.luminaai.port.EmailAnalysisPort;
import com.luminaai.port.EmailFetcherPort;
import com.luminaai.port.NotificationPort;
import com.luminaai.repository.ActionTaskRepository;
import com.luminaai.repository.BriefingRunRepository;
import com.luminaai.repository.ProcessedEmailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyBriefingServiceTest {

    @Mock EmailFetcherPort emailFetcher;
    @Mock EmailAnalysisPort emailAnalysis;
    @Mock NotificationPort notification;
    @Mock BriefingFormatter formatter;
    @Mock BriefingRunRepository briefingRunRepository;
    @Mock ActionTaskRepository actionTaskRepository;
    @Mock ProcessedEmailRepository processedEmailRepository;

    private DailyBriefingService service;

    @BeforeEach
    void setUp() {
        BriefingRun savedRun = BriefingRun.builder()
                .runDate(LocalDate.now())
                .build();
        when(briefingRunRepository.save(any())).thenReturn(savedRun);

        service = new DailyBriefingService(
                emailFetcher, emailAnalysis, notification, formatter,
                briefingRunRepository, actionTaskRepository, processedEmailRepository);
    }

    @Test
    void runsFullPipeline_whenNewEmailsArePresent() {
        EmailMessage email = EmailMessage.builder()
                .id("e1").subject("Hello").from("a@b.com").body("body").build();
        when(emailFetcher.fetchEmailsFromLast24Hours()).thenReturn(List.of(email));
        when(processedEmailRepository.existsByEmailId("e1")).thenReturn(false);

        AnalysisResult analysis = new AnalysisResult();
        analysis.setSummary("All good");
        analysis.setTasks(List.of());
        when(emailAnalysis.analyze(any())).thenReturn(analysis);
        when(formatter.format(any(), any(), anyInt())).thenReturn("briefing text");

        service.runDailyBriefing();

        verify(emailAnalysis).analyze(List.of(email));
        verify(notification).send("briefing text");
        verify(processedEmailRepository).save(any());
    }

    @Test
    void sendsNoNewEmailsMessage_whenAllEmailsAlreadyProcessed() {
        EmailMessage email = EmailMessage.builder()
                .id("e1").subject("Hi").from("a@b.com").body("body").build();
        when(emailFetcher.fetchEmailsFromLast24Hours()).thenReturn(List.of(email));
        when(processedEmailRepository.existsByEmailId("e1")).thenReturn(true);

        service.runDailyBriefing();

        verifyNoInteractions(emailAnalysis);
        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(notification).send(msgCaptor.capture());
        assertThat(msgCaptor.getValue()).contains("No new emails");
    }

    @Test
    void sendsNoEmailsMessage_whenInboxIsEmpty() {
        when(emailFetcher.fetchEmailsFromLast24Hours()).thenReturn(List.of());

        service.runDailyBriefing();

        verifyNoInteractions(emailAnalysis);
        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(notification).send(msgCaptor.capture());
        assertThat(msgCaptor.getValue()).contains("No new emails");
    }

    @Test
    void recordsFailedRun_whenEmailFetcherThrows() {
        when(emailFetcher.fetchEmailsFromLast24Hours()).thenThrow(new RuntimeException("network error"));

        service.runDailyBriefing();

        verifyNoInteractions(notification);
        // BriefingRun is saved twice: once on init (RUNNING) and once on failure (FAILED)
        verify(briefingRunRepository, times(2)).save(any());
    }
}
