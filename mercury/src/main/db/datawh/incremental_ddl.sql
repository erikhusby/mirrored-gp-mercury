-------------------------------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/RPT-3131
-- Mercury QC DM structural changes (Lab_Metric)

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
  well             VARCHAR2(255)
);


drop table lab_metric;

CREATE TABLE lab_metric (
  lab_metric_id    NUMERIC(19) NOT NULL PRIMARY KEY,
  quant_type       VARCHAR2(255),
  quant_units      VARCHAR2(255),
  quant_value      NUMBER(19,2),
  run_name         VARCHAR2(255),
  run_date         DATE,
  lab_vessel_id    NUMERIC(19) NOT NULL,
  well             VARCHAR2(255),
  etl_date         DATE NOT NULL
);
