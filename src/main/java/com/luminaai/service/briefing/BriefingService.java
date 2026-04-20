package com.luminaai.service.briefing;

/**
 * Orchestrates a full daily email briefing cycle: fetch → analyse → persist → notify.
 */
public interface BriefingService {
    void runDailyBriefing();
}
