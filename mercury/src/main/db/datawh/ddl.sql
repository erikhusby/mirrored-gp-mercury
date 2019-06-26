----------------------------------------
-- do not run this on the production db
----------------------------------------

EXEC drop_all_tables;

--   Creates the user-visible tables

CREATE TABLE product (
  product_id                NUMERIC(19) PRIMARY KEY NOT NULL,
  product_name              VARCHAR2(255)           NOT NULL,
  part_number               VARCHAR2(255)           NOT NULL,
  availability_date         DATE                    NOT NULL,
  discontinued_date         DATE,
  expected_cycle_time_sec   NUMERIC(19),
  guaranteed_cycle_time_sec NUMERIC(19),
  samples_per_week          NUMERIC(19),
  is_top_level_product      CHAR(1) CHECK (is_top_level_product IN ('T', 'F')),
  workflow_name             VARCHAR2(255),
  product_family_name       VARCHAR2(255),
  primary_price_item_id     NUMERIC(19, 0),
  aggregation_data_type     VARCHAR2(255),
  etl_date                  DATE                    NOT NULL
);

CREATE TABLE price_item (
  price_item_id   NUMERIC(19) PRIMARY KEY NOT NULL,
  platform        VARCHAR2(255)           NOT NULL,
  category        VARCHAR2(255)           NOT NULL,
  price_item_name VARCHAR2(255)           NOT NULL,
  quote_server_id VARCHAR2(255)           NOT NULL,
  price           NUMERIC(10, 4),
  units           VARCHAR2(80),
  etl_date        DATE                    NOT NULL
);


CREATE TABLE research_project (
  research_project_id        NUMERIC(19) PRIMARY KEY NOT NULL,
  current_status             VARCHAR2(40)            NOT NULL,
  created_date               DATE                    NOT NULL,
  title                      VARCHAR2(255)           NOT NULL,
  irb_not_engaged            CHAR(1) CHECK (irb_not_engaged IN ('T', 'F')),
  jira_ticket_key            VARCHAR2(255),
  parent_research_project_id NUMERIC(19),
  root_research_project_id   NUMERIC(19),
  etl_date                   DATE                    NOT NULL
);

CREATE TABLE research_project_status (
  research_project_id NUMERIC(19)  NOT NULL,
  status_date         DATE         NOT NULL,
  status              VARCHAR2(40) NOT NULL,
  etl_date            DATE         NOT NULL,
  PRIMARY KEY (research_project_id, status_date)
);

CREATE TABLE research_project_person (
  research_project_person_id NUMERIC(19) PRIMARY KEY NOT NULL,
  research_project_id        NUMERIC(19)             NOT NULL,
  project_role               VARCHAR2(80)            NOT NULL,
  person_id                  NUMERIC(19)             NOT NULL,
  first_name                 VARCHAR2(255),
  last_name                  VARCHAR2(255),
  username                   VARCHAR2(255),
  etl_date                   DATE                    NOT NULL
);

CREATE TABLE research_project_funding (
  research_project_funding_id NUMERIC(19)   NOT NULL PRIMARY KEY,
  research_project_id         NUMERIC(19)   NOT NULL,
  funding_id                  VARCHAR2(255) NOT NULL,
  etl_date                    DATE          NOT NULL
);

CREATE TABLE research_project_cohort (
  research_project_cohort_id NUMERIC(19) NOT NULL PRIMARY KEY,
  research_project_id        NUMERIC(19) NOT NULL,
  etl_date                   DATE        NOT NULL
);

CREATE TABLE research_project_irb (
  research_project_irb_id   NUMERIC(19)   NOT NULL PRIMARY KEY,
  research_project_id       NUMERIC(19)   NOT NULL,
  research_project_irb      VARCHAR2(255) NOT NULL,
  research_project_irb_type VARCHAR2(255) NOT NULL,
  etl_date                  DATE          NOT NULL
);

CREATE TABLE product_order (
  product_order_id       NUMERIC(19)    PRIMARY KEY NOT NULL,
  research_project_id    NUMERIC(19),
  product_id             NUMERIC(19),
  status                 VARCHAR2(40)   NOT NULL,
  created_date           DATE,
  modified_date          DATE,
  title                  VARCHAR2(255),
  quote_id               VARCHAR2(255),
  jira_ticket_key        VARCHAR2(255),
  owner                  VARCHAR2(40),
  placed_date            DATE,
  skip_regulatory_reason VARCHAR2(255),
  sap_order_number VARCHAR2(255),
  etl_date               DATE           NOT NULL
);

CREATE TABLE product_order_status (
  product_order_id NUMERIC(19)  NOT NULL,
  status_date      DATE         NOT NULL,
  status           VARCHAR2(40) NOT NULL,
  etl_date         DATE         NOT NULL,
  PRIMARY KEY (product_order_id, status_date)
);

CREATE TABLE product_order_sample (
  product_order_sample_id NUMERIC(19) PRIMARY KEY NOT NULL,
  product_order_id        NUMERIC(19)             NOT NULL,
  sample_name             VARCHAR2(255),
  delivery_status         VARCHAR2(40)            NOT NULL,
  sample_position         NUMERIC(19)             NOT NULL,
  PARTICIPANT_ID          VARCHAR2(255)           NULL,
  SAMPLE_TYPE             VARCHAR2(255)           NULL,
  SAMPLE_RECEIPT          DATE                    NULL,
  ORIGINAL_SAMPLE_TYPE    VARCHAR2(255)           NULL,
  on_risk                 CHAR(1) DEFAULT 'F'     NOT NULL CHECK (on_risk IN ('T', 'F')),
  is_billed               CHAR(1) DEFAULT 'F'     NOT NULL CHECK (is_billed IN ('T', 'F')),
  is_abandoned            CHAR(1) GENERATED ALWAYS AS (CASE WHEN delivery_status = 'ABANDONED' THEN 'T'
  ELSE 'F' END) VIRTUAL,
  ledger_quote_id         VARCHAR2(255),
  etl_date                DATE                    NOT NULL,
  risk_types              VARCHAR2(255),
  risk_messages           VARCHAR2(500)
);

CREATE TABLE product_order_sample_status (
  product_order_sample_id NUMERIC(19)  NOT NULL,
  status_date             DATE         NOT NULL,
  delivery_status         VARCHAR2(40) NOT NULL,
  etl_date                DATE         NOT NULL,
  PRIMARY KEY (product_order_sample_id, status_date)
);

CREATE TABLE product_order_add_on (
  product_order_add_on_id NUMERIC(19) NOT NULL PRIMARY KEY,
  product_order_id        NUMERIC(19) NOT NULL,
  product_id              NUMERIC(19) NOT NULL,
  etl_date                DATE        NOT NULL
);

CREATE TABLE regulatory_info (
  regulatory_info_id NUMERIC(19) NOT NULL PRIMARY KEY,
  identifier         VARCHAR2(255),
  type               VARCHAR2(255),
  name               VARCHAR2(255),
  etl_date           DATE          NOT NULL
);

CREATE TABLE pdo_regulatory_infos (
  product_order    NUMERIC(19)   NOT NULL,
  regulatory_infos NUMERIC(19)   NOT NULL,
  etl_date         DATE          NOT NULL,
  constraint pk_pdo_regulatory_infos PRIMARY KEY ( product_order, regulatory_infos )
);

CREATE TABLE lab_vessel (
  lab_vessel_id   NUMERIC(19)  NOT NULL PRIMARY KEY,
  label           VARCHAR2(40) NOT NULL,
  lab_vessel_type VARCHAR2(40) NOT NULL,
  etl_date        DATE         NOT NULL
);

CREATE TABLE workflow (
  workflow_id      NUMERIC(19)   NOT NULL PRIMARY KEY,
  workflow_name    VARCHAR2(255) NOT NULL,
  workflow_version VARCHAR2(40)  NOT NULL,
  etl_date         DATE          NOT NULL
);

CREATE TABLE workflow_process (
  process_id      NUMERIC(19)   NOT NULL PRIMARY KEY,
  process_name    VARCHAR2(255) NOT NULL,
  process_version VARCHAR2(40)  NOT NULL,
  step_name       VARCHAR2(255) NOT NULL,
  event_name      VARCHAR2(255) NOT NULL,
  etl_date        DATE          NOT NULL
);

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

CREATE TABLE sequencing_sample_fact (
  sequencing_sample_fact_id   NUMERIC(19)   NOT NULL PRIMARY KEY,
  flowcell_barcode            VARCHAR2(255) NOT NULL,
  lane                        VARCHAR2(40) NOT NULL,
  molecular_indexing_scheme   VARCHAR2(255) NOT NULL,
  sequencing_run_id           NUMERIC(19)   NOT NULL,
  product_order_id            NUMERIC(19),
  sample_name                 VARCHAR2(40),
  research_project_id         NUMERIC(19),
  loaded_library_barcode      VARCHAR2(255),
  loaded_library_create_date  DATE,
  batch_name       	      VARCHAR(40),
  etl_date                    DATE          NOT NULL
);

CREATE TABLE sequencing_run (
  sequencing_run_id     NUMERIC(19) NOT NULL PRIMARY KEY,
  run_name              VARCHAR2(255),
  barcode               VARCHAR2(255),
  registration_date     DATE,
  instrument            VARCHAR2(255),
  setup_read_structure  VARCHAR2(255),
  actual_read_structure VARCHAR2(255),
  etl_date              DATE        NOT NULL
);

CREATE TABLE ledger_entry (
  ledger_id               NUMERIC(19) NOT NULL PRIMARY KEY,
  product_order_sample_id NUMERIC(19),
  quote_id                VARCHAR2(255),
  price_item_id           NUMERIC(19),
  price_item_type         VARCHAR2(50),
  quantity                NUMBER(15,4),
  billing_session_id      NUMERIC(19),
  billing_message         VARCHAR2(500),
  work_complete_date      DATE,
  etl_date                DATE        NOT NULL,
  quote_server_work_item  VARCHAR2(255),
  sap_delivery_document  VARCHAR2(255)
);

CREATE TABLE billing_session (
  billing_session_id      NUMERIC(19) NOT NULL PRIMARY KEY,
  billed_date             DATE,
  billing_session_type    VARCHAR2(50),
  etl_date                DATE        NOT NULL
);

CREATE TABLE lab_metric (
  lab_metric_id    NUMERIC(19) NOT NULL,
  quant_type       VARCHAR2(255),
  quant_units      VARCHAR2(255),
  quant_value      NUMBER(19,2),
  run_name         VARCHAR2(255),
  run_date         DATE,
  lab_vessel_id    NUMERIC(19),
  vessel_barcode   VARCHAR2(40) NOT NULL,
  rack_position    VARCHAR2(255),
  decision         VARCHAR2(12),
  decision_date    DATE,
  decider          VARCHAR2(255),
  override_reason  VARCHAR2(255),
  etl_date         DATE NOT NULL,
  constraint PK_LAB_METRIC PRIMARY KEY ( lab_metric_id )
);

CREATE TABLE LIBRARY_LCSET_SAMPLE_BASE(
  LIBRARY_LABEL VARCHAR2(40) NOT NULL,
  LIBRARY_ID NUMBER(19) NOT NULL,
  LIBRARY_TYPE VARCHAR2(255) NOT NULL,
  LIBRARY_CREATION_DATE DATE NOT NULL,
  LIBRARY_EVENT_ID NUMBER(19) NOT NULL );

CREATE OR REPLACE VIEW LIBRARY_LCSET_SAMPLE
AS
  SELECT SB.LIBRARY_LABEL
    --, SB.LIBRARY_ID
    , EF.POSITION
    , EF.LCSET_SAMPLE_NAME AS LCSET_SAMPLE
    , EF.BATCH_NAME
    , EF.MOLECULAR_INDEXING_SCHEME AS MOLECULAR_BARCODE
    , PDO.JIRA_TICKET_KEY AS PRODUCT_ORDER_KEY
    , EF.SAMPLE_NAME AS PRODUCT_ORDER_SAMPLE
    , SB.LIBRARY_TYPE
    , SB.LIBRARY_CREATION_DATE
  --, SB.LIBRARY_EVENT_ID
  FROM LIBRARY_LCSET_SAMPLE_BASE SB
    , EVENT_FACT EF
    , PRODUCT_ORDER PDO
  WHERE EF.LAB_EVENT_ID = SB.LIBRARY_EVENT_ID
        AND EF.LAB_VESSEL_ID = SB.LIBRARY_ID
        AND PDO.PRODUCT_ORDER_ID(+) = EF.PRODUCT_ORDER_ID
        AND EF.LCSET_SAMPLE_NAME IS NOT NULL;


--   Creates the import tables

CREATE TABLE im_product (
  line_number               NUMERIC(9)  NOT NULL,
  etl_date                  DATE        NOT NULL,
  is_delete                 CHAR(1)     NOT NULL,
  product_id                NUMERIC(19) NOT NULL,
  product_name              VARCHAR2(255),
  part_number               VARCHAR2(255),
  availability_date         DATE,
  discontinued_date         DATE,
  expected_cycle_time_sec   NUMERIC(19),
  guaranteed_cycle_time_sec NUMERIC(19),
  samples_per_week          NUMERIC(19),
  is_top_level_product      CHAR(1),
  workflow_name             VARCHAR2(255),
  product_family_name       VARCHAR2(255),
  primary_price_item_id     NUMERIC(19, 0),
  aggregation_data_type	    VARCHAR2(255),
  external_only_product     CHAR(1),
  saved_in_sap              CHAR(1)
);

CREATE TABLE im_price_item (
  line_number     NUMERIC(9)  NOT NULL,
  etl_date        DATE        NOT NULL,
  is_delete       CHAR(1)     NOT NULL,
  price_item_id   NUMERIC(19) NOT NULL,
  platform        VARCHAR2(255),
  category        VARCHAR2(255),
  price_item_name VARCHAR2(255),
  quote_server_id VARCHAR2(255),
  price           NUMERIC(10, 4),
  units           VARCHAR2(80)
);


CREATE TABLE im_research_project (
  line_number                NUMERIC(9)  NOT NULL,
  etl_date                   DATE        NOT NULL,
  is_delete                  CHAR(1)     NOT NULL,
  research_project_id        NUMERIC(19) NOT NULL,
  current_status             VARCHAR2(40),
  created_date               DATE,
  title                      VARCHAR2(255),
  irb_not_engaged            CHAR(1) CHECK (irb_not_engaged IN ('T', 'F')),
  jira_ticket_key            VARCHAR2(255),
  parent_research_project_id NUMERIC(19, 0),
  root_research_project_id   NUMERIC(19, 0)
);


CREATE TABLE im_research_project_status (
  line_number         NUMERIC(9)  NOT NULL,
  etl_date            DATE        NOT NULL,
  is_delete           CHAR(1)     NOT NULL,
  research_project_id NUMERIC(19) NOT NULL,
  status_date         DATE,
  status              VARCHAR2(40)
);

CREATE TABLE im_research_project_person (
  line_number                NUMERIC(9)  NOT NULL,
  etl_date                   DATE        NOT NULL,
  is_delete                  CHAR(1)     NOT NULL,
  research_project_person_id NUMERIC(19) NOT NULL,
  research_project_id        NUMERIC(19),
  project_role               VARCHAR2(80),
  person_id                  NUMERIC(19),
  first_name                 VARCHAR2(255),
  last_name                  VARCHAR2(255),
  username                   VARCHAR2(255)
);

CREATE TABLE im_research_project_funding (
  line_number                 NUMERIC(9)  NOT NULL,
  etl_date                    DATE        NOT NULL,
  is_delete                   CHAR(1)     NOT NULL,
  research_project_funding_id NUMERIC(19) NOT NULL,
  research_project_id         NUMERIC(19),
  funding_id                  VARCHAR2(255)
);

CREATE TABLE im_research_project_cohort (
  line_number                NUMERIC(9)  NOT NULL,
  etl_date                   DATE        NOT NULL,
  is_delete                  CHAR(1)     NOT NULL,
  research_project_cohort_id NUMERIC(19) NOT NULL,
  research_project_id        NUMERIC(19)
);

CREATE TABLE im_research_project_irb (
  line_number               NUMERIC(9)  NOT NULL,
  etl_date                  DATE        NOT NULL,
  is_delete                 CHAR(1)     NOT NULL,
  research_project_irb_id   NUMERIC(19) NOT NULL,
  research_project_id       NUMERIC(19),
  research_project_irb      VARCHAR2(255),
  research_project_irb_type VARCHAR2(255)
);

CREATE TABLE im_product_order (
  line_number            NUMERIC(9)  NOT NULL,
  etl_date               DATE        NOT NULL,
  is_delete              CHAR(1)     NOT NULL,
  product_order_id       NUMERIC(19) NOT NULL,
  research_project_id    NUMERIC(19),
  product_id             NUMERIC(19),
  status                 VARCHAR2(40),
  created_date           DATE,
  modified_date          DATE,
  title                  VARCHAR2(255),
  quote_id               VARCHAR2(255),
  jira_ticket_key        VARCHAR2(255),
  owner                  VARCHAR2(40),
  placed_date            DATE,
  skip_regulatory_reason VARCHAR2(255),
  sap_order_number VARCHAR2(255),
  reg_info_ids           VARCHAR2(255)
);

CREATE TABLE im_product_order_status (
  line_number      NUMERIC(9)  NOT NULL,
  etl_date         DATE        NOT NULL,
  is_delete        CHAR(1)     NOT NULL,
  product_order_id NUMERIC(19) NOT NULL,
  status_date      DATE,
  status           VARCHAR2(40)
);

CREATE TABLE im_product_order_sample_stat (
  line_number             NUMERIC(9)  NOT NULL,
  etl_date                DATE        NOT NULL,
  is_delete               CHAR(1)     NOT NULL,
  product_order_sample_id NUMERIC(19) NOT NULL,
  status_date             DATE,
  delivery_status         VARCHAR2(40)
);


CREATE TABLE im_product_order_sample (
  line_number             NUMERIC(9)  NOT NULL,
  etl_date                DATE        NOT NULL,
  is_delete               CHAR(1)     NOT NULL,
  product_order_sample_id NUMERIC(19) NOT NULL,
  product_order_id        NUMERIC(19),
  sample_name             VARCHAR2(255),
  delivery_status         VARCHAR2(40),
  sample_position         NUMERIC(19),
  PARTICIPANT_ID          VARCHAR2(255) NULL,
  SAMPLE_TYPE             VARCHAR2(255) NULL,
  SAMPLE_RECEIPT          DATE          NULL,
  ORIGINAL_SAMPLE_TYPE    VARCHAR2(255) NULL
);

CREATE TABLE im_product_order_add_on (
  line_number             NUMERIC(9)  NOT NULL,
  etl_date                DATE        NOT NULL,
  is_delete               CHAR(1)     NOT NULL,
  product_order_add_on_id NUMERIC(19) NOT NULL,
  product_order_id        NUMERIC(19),
  product_id              NUMERIC(19)
);

CREATE TABLE im_pdo_regulatory_infos (
  line_number      NUMERIC(9)  NOT NULL,
  etl_date         DATE        NOT NULL,
  is_delete        CHAR(1)     NOT NULL,
  product_order    NUMERIC(19),
  regulatory_infos NUMERIC(19)
);

CREATE TABLE im_regulatory_info (
  line_number        NUMERIC(9)    NOT NULL,
  etl_date           DATE          NOT NULL,
  is_delete          CHAR(1)       NOT NULL,
  regulatory_info_id NUMERIC(19),
  identifier         VARCHAR2(255),
  type               VARCHAR2(255),
  name               VARCHAR2(255)
);

CREATE TABLE im_lab_vessel (
  line_number     NUMERIC(9)  NOT NULL,
  etl_date        DATE        NOT NULL,
  is_delete       CHAR(1)     NOT NULL,
  lab_vessel_id   NUMERIC(19) NOT NULL,
  label           VARCHAR2(40),
  lab_vessel_type VARCHAR2(40)
);

CREATE TABLE im_workflow (
  line_number      NUMERIC(9)  NOT NULL,
  etl_date         DATE        NOT NULL,
  is_delete        CHAR(1)     NOT NULL,
  workflow_id      NUMERIC(19) NOT NULL,
  workflow_name    VARCHAR2(255),
  workflow_version VARCHAR2(40)
);

CREATE TABLE im_workflow_process (
  line_number     NUMERIC(9)  NOT NULL,
  etl_date        DATE        NOT NULL,
  is_delete       CHAR(1)     NOT NULL,
  process_id      NUMERIC(19) NOT NULL,
  process_name    VARCHAR2(255),
  process_version VARCHAR2(40),
  step_name       VARCHAR2(255),
  event_name      VARCHAR2(255)
);

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
  EVENT_FACT_ID NUMBER(28) ); --this gets populated by merge_import.sql

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

CREATE TABLE im_product_order_sample_risk (
  line_number             NUMERIC(9)  NOT NULL,
  etl_date                DATE        NOT NULL,
  is_delete               CHAR(1)     NOT NULL,
  product_order_sample_id NUMERIC(19) NOT NULL,
  on_risk                 CHAR(1),
  risk_types              VARCHAR2(255),
  risk_messages           VARCHAR2(500)
);

CREATE TABLE im_product_order_sample_bill (
  line_number             NUMERIC(9)  NOT NULL,
  etl_date                DATE        NOT NULL,
  is_delete               CHAR(1)     NOT NULL,
  product_order_sample_id NUMERIC(19) NOT NULL,
  is_billed               CHAR(1)
);

CREATE TABLE im_ledger_entry (
  line_number             NUMERIC(9)  NOT NULL,
  etl_date                DATE        NOT NULL,
  is_delete               CHAR(1)     NOT NULL,
  ledger_id               NUMERIC(19) NOT NULL,
  product_order_sample_id NUMERIC(19),
  quote_id                VARCHAR2(255),
  price_item_id           NUMERIC(19),
  price_item_type         VARCHAR2(50),
  quantity                NUMBER(15,4),
  billing_session_id      NUMERIC(19),
  billing_message         VARCHAR2(500),
  work_complete_date      DATE,
  quote_server_work_item varchar2(255),
  sap_delivery_document  VARCHAR2(255)
);

CREATE TABLE im_billing_session (
  line_number             NUMERIC(9)  NOT NULL,
  etl_date                DATE        NOT NULL,
  is_delete               CHAR(1)     NOT NULL,
  billing_session_id      NUMERIC(19) NOT NULL,
  billed_date             DATE,
  billing_session_type    VARCHAR2(50)
);

CREATE TABLE im_sequencing_sample_fact (
  line_number                 NUMERIC(9) NOT NULL,
  etl_date                    DATE       NOT NULL,
  is_delete                   CHAR(1)    NOT NULL,
  sequencing_sample_fact_id   NUMERIC(19),
  flowcell_barcode            VARCHAR2(255),
  lane                        VARCHAR2(40),
  molecular_indexing_scheme   VARCHAR2(255),
  sequencing_run_id           NUMERIC(19),
  product_order_id            NUMERIC(19),
  sample_name                 VARCHAR2(40),
  research_project_id         NUMERIC(19),
  loaded_library_barcode      VARCHAR2(255),
  loaded_library_create_date  DATE,
  batch_name                  VARCHAR(40)
);

CREATE TABLE im_sequencing_run (
  line_number       NUMERIC(9)  NOT NULL,
  etl_date          DATE        NOT NULL,
  is_delete         CHAR(1)     NOT NULL,
  sequencing_run_id NUMERIC(19) NOT NULL,
  run_name          VARCHAR2(255),
  barcode           VARCHAR2(255),
  registration_date DATE,
  instrument        VARCHAR2(255),
  setup_read_structure  VARCHAR2(255),
  actual_read_structure VARCHAR2(255)
);

CREATE TABLE im_lab_metric (
  line_number      NUMERIC(9)  NOT NULL,
  etl_date         DATE        NOT NULL,
  is_delete        CHAR(1)     NOT NULL,
  lab_metric_id    NUMERIC(19) NOT NULL,
  quant_type       VARCHAR2(255),
  quant_units      VARCHAR2(255),
  quant_value      NUMBER(19,2),
  run_name         VARCHAR2(255),
  run_date         DATE,
  lab_vessel_id    NUMERIC(19),
  vessel_barcode   VARCHAR2(40),
  rack_position    VARCHAR2(255),
  decision         VARCHAR2(12),
  decision_date    DATE,
  decider          VARCHAR2(255),
  override_reason  VARCHAR2(255)
);


CREATE SEQUENCE event_fact_id_seq START WITH 1;
CREATE SEQUENCE sequencing_sample_id_seq START WITH 1;


--  Creates foreign key constraints

ALTER TABLE research_project_status ADD CONSTRAINT fk_rp_status_rpid FOREIGN KEY (research_project_id)
REFERENCES research_project (research_project_id) ON DELETE CASCADE;

ALTER TABLE research_project_person ADD CONSTRAINT fk_rp_person_rpid FOREIGN KEY (research_project_id)
REFERENCES research_project (research_project_id) ON DELETE CASCADE;

ALTER TABLE research_project_funding ADD CONSTRAINT fk_rp_funding_rpid FOREIGN KEY (research_project_id)
REFERENCES research_project (research_project_id) ON DELETE CASCADE;

ALTER TABLE research_project_cohort ADD CONSTRAINT fk_rp_cohort_rpid FOREIGN KEY (research_project_id)
REFERENCES research_project (research_project_id) ON DELETE CASCADE;

ALTER TABLE research_project_irb ADD CONSTRAINT fk_rp_irb_rpid FOREIGN KEY (research_project_id)
REFERENCES research_project (research_project_id) ON DELETE CASCADE;

ALTER TABLE product_order ADD CONSTRAINT fk_po_rpid FOREIGN KEY (research_project_id)
REFERENCES research_project (research_project_id) ON DELETE CASCADE;

ALTER TABLE product_order ADD CONSTRAINT fk_po_productid FOREIGN KEY (product_id)
REFERENCES product (product_id) ON DELETE CASCADE;

ALTER TABLE product_order_status ADD CONSTRAINT fk_po_status_poid FOREIGN KEY (product_order_id)
REFERENCES product_order (product_order_id) ON DELETE CASCADE;

ALTER TABLE product_order_sample ADD CONSTRAINT fk_pos_poid FOREIGN KEY (product_order_id)
REFERENCES product_order (product_order_id) ON DELETE CASCADE;

ALTER TABLE product_order_sample_status ADD CONSTRAINT fk_po_sample_b_s_po_sid FOREIGN KEY (product_order_sample_id)
REFERENCES product_order_sample (product_order_sample_id) ON DELETE CASCADE;

ALTER TABLE product_order_add_on ADD CONSTRAINT fk_po_add_on_prodid FOREIGN KEY (product_id)
REFERENCES product (product_id) ON DELETE CASCADE;

ALTER TABLE product_order_add_on ADD CONSTRAINT fk_po_add_on_poid FOREIGN KEY (product_order_id)
REFERENCES product_order (product_order_id) ON DELETE CASCADE;

ALTER TABLE product ADD CONSTRAINT fk_product_price_item_id FOREIGN KEY (primary_price_item_id)
REFERENCES price_item (price_item_id) ON DELETE CASCADE;

ALTER TABLE sequencing_sample_fact ADD CONSTRAINT fk_seq_sample_seqrun_id FOREIGN KEY (sequencing_run_id)
REFERENCES sequencing_run (sequencing_run_id) ON DELETE CASCADE;

ALTER TABLE sequencing_sample_fact ADD CONSTRAINT fk_seq_sample_pdo_id FOREIGN KEY (product_order_id)
REFERENCES product_order (product_order_id) ON DELETE CASCADE;

ALTER TABLE sequencing_sample_fact ADD CONSTRAINT fk_seq_sample_rpid FOREIGN KEY (research_project_id)
REFERENCES research_project (research_project_id) ON DELETE CASCADE;

alter table pdo_regulatory_infos
add constraint FK_PDO_REGINFO
foreign key(product_order)
references product_order(product_order_id) ON DELETE CASCADE;

alter table pdo_regulatory_infos
add constraint FK_REGINFO_PDO
foreign key(regulatory_infos)
references regulatory_info(regulatory_info_id) ON DELETE CASCADE;

--  Creates indexes

CREATE INDEX research_project_status_idx1 ON research_project_status (research_project_id);
CREATE INDEX research_project_person_idx1 ON research_project_person (research_project_id);
CREATE INDEX research_project_fund_idx1 ON research_project_funding (research_project_id);
CREATE INDEX research_project_cohort_idx1 ON research_project_cohort (research_project_id);
CREATE INDEX lab_metric_vessel_idx1 ON lab_metric (vessel_barcode);
CREATE INDEX research_project_irb_idx1 ON research_project_irb (research_project_id);
CREATE INDEX product_order_idx1 ON product_order (research_project_id);
CREATE INDEX product_order_idx2 ON product_order (product_id);
CREATE INDEX product_order_status_idx1 ON product_order_status (product_order_id);
CREATE UNIQUE INDEX product_order_sample_idx1 ON product_order_sample (product_order_id, sample_name, sample_position);
CREATE INDEX pdo_sample_status_idx1 ON product_order_sample_status (product_order_sample_id);
CREATE INDEX pdo_add_on_idx1 ON product_order_add_on (product_order_id);
CREATE INDEX pdo_add_on_idx2 ON product_order_add_on (product_id);
CREATE INDEX event_fact_idx1 ON event_fact (event_date);
CREATE INDEX event_fact_idx2 ON event_fact (product_order_id, sample_name);
CREATE INDEX event_fact_idx3 ON event_fact (lab_event_id);
CREATE INDEX IDX_EVENT_VESSEL ON EVENT_FACT( LAB_VESSEL_ID );
CREATE UNIQUE INDEX PK_ANCESTRY on library_ancestry (child_library_id, ancestor_library_id );
ALTER TABLE library_ancestry ADD CONSTRAINT PK_ANCESTRY PRIMARY KEY (child_library_id, ancestor_library_id ) USING INDEX PK_ANCESTRY;
CREATE UNIQUE INDEX idx_ancestry_reverse on library_ancestry (ancestor_library_id, child_library_id );
CREATE INDEX ix_parent_project ON research_project (parent_research_project_id);
CREATE INDEX ix_root_project ON research_project (root_research_project_id);
CREATE UNIQUE INDEX seq_sample_fact_idx1 ON sequencing_sample_fact (flowcell_barcode, lane, molecular_indexing_scheme);
CREATE INDEX seq_sample_fact_idx2 ON sequencing_sample_fact (product_order_id, sample_name);
CREATE INDEX seq_sample_fact_idx3 ON sequencing_sample_fact (sequencing_run_id);
CREATE INDEX pdo_regulatory_info_idx1 ON pdo_regulatory_infos (regulatory_infos);
-- Warehouse ancestry query performance
CREATE UNIQUE INDEX IDX_VESSEL_LABEL ON LAB_VESSEL(LABEL);
-- Ancestry ETL delete performance
CREATE INDEX IDX_ANCESTRY_CHILD_EVENT ON LIBRARY_ANCESTRY( CHILD_EVENT_ID );
CREATE INDEX IDX_LIBRARY_LABEL_SAMPLE ON LIBRARY_LCSET_SAMPLE_BASE ( LIBRARY_LABEL, LIBRARY_TYPE );
CREATE INDEX IDX_LIBRARY_ID_SAMPLE ON LIBRARY_LCSET_SAMPLE_BASE ( LIBRARY_ID, LIBRARY_TYPE );

