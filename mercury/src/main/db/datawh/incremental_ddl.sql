-- --------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4934
-- Add call rate threshold to mercury DW ETL
-- --------------------------------

ALTER TABLE IM_PRODUCT_ORDER ADD CALL_RATE_THRESHOLD VARCHAR2(12);
ALTER TABLE PRODUCT_ORDER ADD CALL_RATE_THRESHOLD VARCHAR2(12);
