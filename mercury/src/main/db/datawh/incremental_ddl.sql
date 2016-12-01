-------------------------------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4085
-- Add ancestry relationships for DenatureToDilutionTransfer events

-- Uncomment and execute when ready to clear existing data prior to full refresh of related tables:

--truncate table event_fact;
--truncate table library_ancestry;
--truncate table library_lcset_sample_base;
--drop sequence event_fact_id_seq;
--CREATE SEQUENCE event_fact_id_seq START WITH 1;


-------------------------------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/RPT-3131
-- Mercury QC DM structural changes (Lab_Metric)

-- Prior to run:
-- Create backfill files from PROD release branch - record latest lab_metric_id value in last file (IDs not sequential in file - sort required)
-- Copy lab_metric.ctl file

Alter table im_product_order add column sap_order_number VARCHAR(255);

ALTER TABLE im_product add column external_only_product char(1);
ALTER TABLE im_product add column saved_in_sap char(1);

-- After run:
-- Execute merge_import.sql
-- Copy backfill files to datawh/prod/new folder
-- Execute backfill rest call against prod for all id's greater than the one recorded at pre-deploy backfill
