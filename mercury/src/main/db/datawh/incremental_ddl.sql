-------------------------------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-3557
-- Create Ancestry ETL
-------------------------------------------------------

DROP TABLE IM_EVENT_FACT;

CREATE TABLE MERCURYDW.IM_EVENT_FACT (
  LINE_NUMBER NUMBER(9) NOT NULL,
  ETL_DATE DATE NOT NULL,
  IS_DELETE CHAR NOT NULL,
  LAB_EVENT_ID NUMBER(19),
  WORKFLOW_ID NUMBER(19),
  PROCESS_ID NUMBER(19),
  LAB_EVENT_TYPE VARCHAR2(64),
  PRODUCT_ORDER_ID NUMBER(19),
  SAMPLE_NAME VARCHAR2(40),
  LCSET_SAMPLE_NAME VARCHAR2(40),
  BATCH_NAME VARCHAR2(40),
  STATION_NAME VARCHAR2(255),
  LAB_VESSEL_ID NUMBER(19),
  POSITION VARCHAR2(32),
  PROGRAM_NAME VARCHAR2(255),
  MOLECULAR_INDEXING_SCHEME VARCHAR2(255),
  EVENT_DATE DATE,
  EVENT_FACT_ID NUMBER(28) );

ALTER TABLE MERCURYDW.EVENT_FACT RENAME TO EVENT_FACT_BAK;
ALTER TABLE MERCURYDW.EVENT_FACT_BAK DROP CONSTRAINT FK_EVENT_LAB_VESSEL;
ALTER TABLE MERCURYDW.EVENT_FACT_BAK DROP CONSTRAINT FK_EVENT_PDO;
ALTER TABLE MERCURYDW.EVENT_FACT_BAK DROP CONSTRAINT FK_EVENT_WORKFLOW;
ALTER TABLE MERCURYDW.EVENT_FACT_BAK DROP CONSTRAINT FK_EVENT_PROCESS;
DROP INDEX MERCURYDW.EVENT_FACT_IDX1;
DROP INDEX MERCURYDW.EVENT_FACT_IDX2;
DROP INDEX MERCURYDW.EVENT_FACT_IDX3;
-- Ignore not exists error
DROP INDEX MERCURYDW.IDX_EVENT_VESSEL;

CREATE TABLE MERCURYDW.EVENT_FACT(
  EVENT_FACT_ID NUMBER(28) NOT NULL,
  LAB_EVENT_ID NUMBER(19) NOT NULL,
  WORKFLOW_ID NUMBER(19),
  PROCESS_ID NUMBER(19),
  LAB_EVENT_TYPE VARCHAR2(64),
  PRODUCT_ORDER_ID NUMBER(19),
  SAMPLE_NAME VARCHAR2(40),
  LCSET_SAMPLE_NAME VARCHAR2(40),
  BATCH_NAME VARCHAR2(40),
  STATION_NAME VARCHAR2(255),
  LAB_VESSEL_ID NUMBER(19),
  POSITION VARCHAR2(32),
  EVENT_DATE DATE NOT NULL,
  PROGRAM_NAME VARCHAR2(255),
  MOLECULAR_INDEXING_SCHEME VARCHAR2(255),
  ETL_DATE DATE NOT NULL,
  PRIMARY KEY (EVENT_FACT_ID),
  CONSTRAINT FK_EVENT_LAB_VESSEL FOREIGN KEY (LAB_VESSEL_ID)
  REFERENCES MERCURYDW.LAB_VESSEL (LAB_VESSEL_ID) ON DELETE CASCADE ENABLE,
  CONSTRAINT FK_EVENT_PDO FOREIGN KEY (PRODUCT_ORDER_ID)
  REFERENCES MERCURYDW.PRODUCT_ORDER (PRODUCT_ORDER_ID) ON DELETE CASCADE ENABLE,
  CONSTRAINT FK_EVENT_WORKFLOW FOREIGN KEY (WORKFLOW_ID)
  REFERENCES MERCURYDW.WORKFLOW (WORKFLOW_ID) ON DELETE CASCADE ENABLE,
  CONSTRAINT FK_EVENT_PROCESS FOREIGN KEY (PROCESS_ID)
  REFERENCES MERCURYDW.WORKFLOW_PROCESS (PROCESS_ID) ON DELETE CASCADE ENABLE );

DROP SEQUENCE event_fact_id_seq;
CREATE SEQUENCE event_fact_id_seq START WITH 1;

CREATE INDEX MERCURYDW.EVENT_FACT_IDX1 ON MERCURYDW.EVENT_FACT (EVENT_DATE);
CREATE INDEX MERCURYDW.EVENT_FACT_IDX2 ON MERCURYDW.EVENT_FACT (PRODUCT_ORDER_ID, SAMPLE_NAME);
CREATE INDEX MERCURYDW.EVENT_FACT_IDX3 ON MERCURYDW.EVENT_FACT (LAB_EVENT_ID);
CREATE INDEX MERCURYDW.IDX_EVENT_VESSEL ON MERCURYDW.EVENT_FACT (LAB_VESSEL_ID);


CREATE TABLE im_library_ancestry
(
  line_number               NUMERIC(9) NOT NULL,
  etl_date                  DATE NOT NULL,
  is_delete                 CHAR(1) NOT NULL,
  ancestor_event_id         NUMBER(19) NOT NULL,
  ancestor_library_id       NUMBER(19) NOT NULL,
  ancestor_library_type     VARCHAR2(255) NOT NULL,
  ancestor_library_creation DATE NOT NULL,
  child_event_id            NUMBER(19) NOT NULL,
  child_library_id          NUMBER(19) NOT NULL,
  child_library_type        VARCHAR2(255) NOT NULL,
  child_library_creation    DATE NOT NULL
);

CREATE TABLE library_ancestry
(
  ancestor_event_id         NUMBER(19) NOT NULL,
  ancestor_library_id       NUMBER(19) NOT NULL,
  ancestor_library_type     VARCHAR2(255) NOT NULL,
  ancestor_library_creation DATE NOT NULL,
  child_event_id            NUMBER(19) NOT NULL,
  child_library_id          NUMBER(19) NOT NULL,
  child_library_type        VARCHAR2(255) NOT NULL,
  child_library_creation    DATE NOT NULL,
  etl_date                  DATE NOT NULL
);

CREATE UNIQUE INDEX PK_ANCESTRY on library_ancestry (child_library_id, ancestor_library_id, child_event_id, ancestor_event_id );
CREATE INDEX idx_ancestry_reverse on library_ancestry (ancestor_library_id, child_library_id );
CREATE UNIQUE INDEX IDX_VESSEL_LABEL ON LAB_VESSEL( LABEL ) COMPUTE STATISTICS;

-- Warehouse query performance
-- ETL delete performance
CREATE INDEX IDX_ANCESTRY_CHILD_EVENT ON LIBRARY_ANCESTRY( CHILD_EVENT_ID );
DROP PROCEDURE MERGE_IMPORT;
