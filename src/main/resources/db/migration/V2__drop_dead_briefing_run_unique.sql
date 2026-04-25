-- ============================================================
-- LUMINA AI – V2: drop dead UNIQUE(run_date, started_at) on briefing_runs
-- ============================================================
-- The constraint added in V1 is vacuous: started_at is set per row at insert
-- (DEFAULT NOW()), so the pair is effectively unique by virtue of the
-- timestamp alone. The intent (one successful run per day) is enforced in
-- application code via BriefingRunRepository.findByRunDateAndStatus.

ALTER TABLE briefing_runs DROP CONSTRAINT IF EXISTS uq_briefing_run_date;
