-------------------------------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-3557
-- Create Ancestry ETL
-------------------------------------------------------

ALTER TABLE IM_EVENT_FACT
ADD ( molecular_indexing_scheme   VARCHAR2(255)
);

ALTER TABLE MERCURYDW.EVENT_FACT RENAME TO EVENT_FACT_BAK;
ALTER TABLE MERCURYDW.EVENT_FACT_BAK DROP CONSTRAINT FK_EVENT_LAB_VESSEL;
ALTER TABLE MERCURYDW.EVENT_FACT_BAK DROP CONSTRAINT FK_EVENT_PDO;
ALTER TABLE MERCURYDW.EVENT_FACT_BAK DROP CONSTRAINT FK_EVENT_WORKFLOW;
ALTER TABLE MERCURYDW.EVENT_FACT_BAK DROP CONSTRAINT FK_EVENT_PROCESS;
DROP INDEX INDEX MERCURYDW.EVENT_FACT_IDX1;
DROP INDEX MERCURYDW.EVENT_FACT_IDX2;
DROP INDEX MERCURYDW.EVENT_FACT_IDX3;
DROP INDEX MERCURYDW.IDX_EVENT_VESSEL;


CREATE TABLE MERCURYDW.EVENT_FACT
(	EVENT_FACT_ID NUMBER(28,0) NOT NULL,
   LAB_EVENT_ID NUMBER(19) NOT NULL,
   WORKFLOW_ID NUMBER(19),
   PROCESS_ID NUMBER(19),
   PRODUCT_ORDER_ID NUMBER(19),
   SAMPLE_NAME VARCHAR2(40),
   BATCH_NAME VARCHAR2(40),
   STATION_NAME VARCHAR2(255),
   LAB_VESSEL_ID NUMBER(19),
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
  REFERENCES MERCURYDW.WORKFLOW_PROCESS (PROCESS_ID) ON DELETE CASCADE ENABLE;

CREATE INDEX MERCURYDW.EVENT_FACT_IDX1 ON MERCURYDW.EVENT_FACT (EVENT_DATE);
CREATE INDEX MERCURYDW.EVENT_FACT_IDX2 ON MERCURYDW.EVENT_FACT (PRODUCT_ORDER_ID, SAMPLE_NAME);
CREATE INDEX MERCURYDW.EVENT_FACT_IDX3 ON MERCURYDW.EVENT_FACT (LAB_EVENT_ID);
CREATE INDEX MERCURYDW.IDX_EVENT_VESSEL ON MERCURYDW.EVENT_FACT (LAB_VESSEL_ID);

--ALTER TABLE EVENT_FACT
--ADD ( molecular_indexing_scheme   VARCHAR2(255)
--);

CREATE TABLE im_library_ancestry_fact
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

CREATE TABLE library_ancestry_fact
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

CREATE INDEX idx_ancestry_fact_hierarchy on library_ancestry_fact (child_library_id, ancestor_library_id );
CREATE INDEX idx_ancestry_fact_reverse on library_ancestry_fact (ancestor_library_id, child_library_id );
CREATE UNIQUE INDEX IDX_VESSEL_LABEL ON LAB_VESSEL( LABEL ) COMPUTE STATISTICS;

-- Warehouse query performance
-- ETL delete performance
CREATE INDEX IDX_ANCESTRY_CHILD_EVENT ON LIBRARY_ANCESTRY_FACT( CHILD_EVENT_ID );
DROP PROCEDURE MERGE_IMPORT;

--  -------------------------------------------------------
-- Release 1.61
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-3425
-- Create Real time Compliance Report
-------------------------------------------------------
-- ALTER TABLE PRODUCT_ORDER    ADD SKIP_REGULATORY_REASON VARCHAR2(255);
-- ALTER TABLE IM_PRODUCT_ORDER ADD (
--   SKIP_REGULATORY_REASON VARCHAR2(255),
--   REG_INFO_IDS VARCHAR2(255) );

-- DROP TABLE pdo_regulatory_infos;

-- CREATE TABLE pdo_regulatory_infos (
--   product_order    NUMERIC(19)   NOT NULL,
--   regulatory_infos NUMERIC(19)   NOT NULL,
--   etl_date         DATE          NOT NULL,
--   constraint pk_pdo_regulatory_infos PRIMARY KEY ( product_order, regulatory_infos )
-- );

--DROP TABLE regulatory_info;

-- CREATE TABLE regulatory_info (
--   regulatory_info_id NUMERIC(19) NOT NULL PRIMARY KEY,
--   identifier         VARCHAR2(255),
--   type               VARCHAR2(255),
--   name               VARCHAR2(255),
--   etl_date           DATE          NOT NULL
-- );

-- alter table pdo_regulatory_infos
-- add constraint FK_PDO_REGINFO
-- foreign key(product_order)
-- references product_order(product_order_id) ON DELETE CASCADE;

-- alter table pdo_regulatory_infos
-- add constraint FK_REGINFO_PDO
-- foreign key(regulatory_infos)
-- references regulatory_info(regulatory_info_id) ON DELETE CASCADE;

-- CREATE INDEX pdo_regulatory_infos_idx1 ON pdo_regulatory_infos (regulatory_infos);

--DROP TABLE im_regulatory_info;

-- CREATE TABLE im_regulatory_info (
--   line_number        NUMERIC(9)    NOT NULL,
--   etl_date           DATE          NOT NULL,
--   is_delete          CHAR(1)       NOT NULL,
--   regulatory_info_id NUMERIC(19),
--   identifier         VARCHAR2(255),
--   type               VARCHAR2(255),
--   name               VARCHAR2(255)
-- );


-------------------------------------------------------
-- For release 1.49
-------------------------------------------------------
-- alter table ledger_entry add quote_server_work_item varchar2(255);
-- alter table im_ledger_entry add quote_server_work_item varchar2(255);
