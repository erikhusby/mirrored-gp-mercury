-------------------------------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/RPT-3131
-- Mercury QC DM structural changes (Lab_Metric)

-- Prior to run:
-- Create backfill files from PROD release branch - record latest lab_metric_id value in last file (IDs not sequential in file - sort required)
-- Copy lab_metric.ctl file

DROP TABLE im_lab_metric;

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


drop table lab_metric;

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

CREATE INDEX lab_metric_vessel_idx1 ON lab_metric (vessel_barcode);

-- After run:
-- Execute merge_import.sql
-- Copy backfill files to datawh/prod/new folder
-- Execute backfill rest call against prod for all id's greater than the one recorded at pre-deploy backfill
