-- --------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4420
-- Add array chip type to mercury DW ETL
-- --------------------------------

ALTER TABLE IM_PRODUCT_ORDER ADD ARRAY_CHIP_TYPE VARCHAR2(255);
ALTER TABLE PRODUCT_ORDER ADD ARRAY_CHIP_TYPE VARCHAR2(255);

