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

-- --------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4605
-- Add modification timestamp to DW ETL of array process
-- --------------------------------
ALTER TABLE ARRAY_PROCESS_FLOW
  ADD ETL_MOD_TIMESTAMP DATE DEFAULT SYSDATE NOT NULL;

CREATE INDEX IDX_ARRAY_PROCESS_MOD_TS ON ARRAY_PROCESS_FLOW (ETL_MOD_TIMESTAMP);


-- --------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4540
-- Add abandoned plastic data to DW ETL process
-- --------------------------------

DROP TABLE ABANDON_VESSEL;

CREATE TABLE ABANDON_VESSEL (
  ABANDON_TYPE VARCHAR2(24) NOT NULL,
  ABANDON_ID NUMBER(19) NOT NULL ENABLE,
  ABANDON_VESSEL_ID NUMBER(19),
  VESSEL_POSITION VARCHAR2(255),
  REASON VARCHAR2(255),
  ABANDONED_ON TIMESTAMP(6),
  ETL_DATE DATE,
  CONSTRAINT PK_ABANDON_VESSEL PRIMARY KEY (ABANDON_TYPE, ABANDON_ID)
    USING INDEX (
      CREATE UNIQUE INDEX PK_ABANDON_VESSEL
        ON ABANDON_VESSEL( ABANDON_TYPE, ABANDON_ID )
    )
);

CREATE INDEX IDX_ABANDON_VESSEL_ID ON ABANDON_VESSEL( ABANDON_VESSEL_ID ) COMPUTE STATISTICS;

DROP TABLE IM_ABANDON_VESSEL;

CREATE TABLE IM_ABANDON_VESSEL (
  LINE_NUMBER NUMBER(9),
  ETL_DATE DATE,
  IS_DELETE CHAR,
  ABANDON_TYPE VARCHAR2(24),
  ABANDON_ID NUMBER(19),
  ABANDON_VESSEL_ID NUMBER(19),
  REASON VARCHAR2(255),
  ABANDONED_ON TIMESTAMP(6)
);

DROP TABLE IM_ABANDON_VESSEL_POSITION;

CREATE TABLE IM_ABANDON_VESSEL_POSITION (
  LINE_NUMBER NUMBER(9),
  ETL_DATE DATE,
  IS_DELETE CHAR,
  ABANDON_TYPE VARCHAR2(24),
  ABANDON_ID NUMBER(19),
  ABANDON_VESSEL_ID NUMBER(19),
  VESSEL_POSITION VARCHAR2(255),
  REASON VARCHAR2(255),
  ABANDONED_ON TIMESTAMP(6)
);



-- --------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4644
-- Add created_on to LabVessel DW ETL process and use for library_lcset_sample library creation date
-- --------------------------------
ALTER TABLE im_lab_vessel add created_on date;
ALTER TABLE lab_vessel add created_on date;

-- Post-deploy required backfill of lab_vessel data
-- Execute the following AFTER backfill:
UPDATE LIBRARY_LCSET_SAMPLE_BASE LC
   SET LC.LIBRARY_CREATION_DATE = NVL((SELECT LV.CREATED_ON FROM LAB_VESSEL LV WHERE LV.LAB_VESSEL_ID = LC.LIBRARY_ID ), LC.LIBRARY_CREATION_DATE);
COMMIT;

