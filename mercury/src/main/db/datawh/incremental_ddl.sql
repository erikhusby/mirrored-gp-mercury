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
  vessel_barcode   VARCHAR2(40),
  rack_position    VARCHAR2(255),
  decision         VARCHAR2(12),
  decision_date    DATE,
  decider          VARCHAR2(255),
  override_reason  VARCHAR2(255)
);


CREATE TABLE lab_metric_new
AS SELECT lab_metric_id,
  quant_type,
  quant_units,
  quant_value,
  run_name,
  run_date,
  ( select lv.label from lab_vessel lv where lv.lab_vessel_id = lab_metric.lab_vessel_id ) as vessel_barcode,
  vessel_position as rack_position,
  cast( null as VARCHAR2(12) ) as decision,
  cast( null as DATE ) as decision_date,
  cast( null as VARCHAR2(255) ) as decider,
  cast( null as VARCHAR2(255) ) as override_reason,
  etl_date
FROM LAB_METRIC;

drop table lab_metric;

alter table lab_metric_new
rename to lab_metric;

alter table lab_metric
add constraint PK_LAB_METRIC PRIMARY KEY ( lab_metric_id );

alter table lab_metric
modify vessel_barcode not null;

CREATE INDEX lab_metric_vessel_idx1 ON lab_metric (vessel_barcode);



