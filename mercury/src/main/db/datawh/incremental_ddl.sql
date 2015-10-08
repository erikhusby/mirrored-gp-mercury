-------------------------------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-3763
-- Add metadata to PRODUCT_ORDER_SAMPLE
-------------------------------------------------------
ALTER TABLE IM_PRODUCT_ORDER_SAMPLE
ADD (
  PARTICIPANT_ID VARCHAR2(255) NULL,
  SAMPLE_TYPE VARCHAR2(255) NULL,
  SAMPLE_RECEIPT DATE NULL,
  ORIGINAL_SAMPLE_TYPE VARCHAR2(255) NULL );

-- Append to end of table
-- todo jms - restructure table to insert new columns in more logical order?
ALTER TABLE PRODUCT_ORDER_SAMPLE
ADD (
  PARTICIPANT_ID VARCHAR2(255) NULL,
  SAMPLE_TYPE VARCHAR2(255) NULL,
  SAMPLE_RECEIPT DATE NULL,
  ORIGINAL_SAMPLE_TYPE VARCHAR2(255) NULL );