-- --------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4420
-- Add array chip type to mercury DW ETL
-- --------------------------------

ALTER TABLE IM_PRODUCT_ORDER ADD ARRAY_CHIP_TYPE VARCHAR2(255);
ALTER TABLE PRODUCT_ORDER ADD ARRAY_CHIP_TYPE VARCHAR2(255);
ALTER TABLE ARRAY_PROCESS_FLOW
  ADD SCANNER VARCHAR2(255);

-- --------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4554
-- Add scanner to DW ETL of array process
-- --------------------------------

UPDATE ARRAY_PROCESS_FLOW
SET SCANNER = (
        SELECT STATION_NAME
        FROM EVENT_FACT
        WHERE LAB_EVENT_ID = AUTOCALL_EVENT_ID
              AND ROWNUM = 1  )
WHERE AUTOCALL_EVENT_ID IS NOT NULL;

COMMIT;

SELECT SCANNER, COUNT(*)
FROM ARRAY_PROCESS_FLOW
GROUP BY SCANNER;

