-------------------------------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4085
-- Add ancestry relationships for DenatureToDilutionTransfer events

-- Uncomment and execute when ready to clear existing data prior to full refresh of related tables:

--truncate table event_fact;
--truncate table library_ancestry;
--truncate table library_lcset_sample_base;
--drop sequence event_fact_id_seq;
--CREATE SEQUENCE event_fact_id_seq START WITH 1;