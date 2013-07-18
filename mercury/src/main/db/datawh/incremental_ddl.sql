-------------------------------------------------------
-- For release 1.28
-------------------------------------------------------
alter table im_event_fact add (batch_name VARCHAR(40));
alter table im_sequencing_sample_fact add (batch_name VARCHAR(40));
alter table sequencing_sample_fact add (batch_name VARCHAR(40));
alter table event_fact add (batch_name VARCHAR(40));

update event_fact ef set ef.batch_name = (select lb.batch_name from lab_batch lb where lb.lab_batch_id = ef.lab_batch_id);

alter table event_fact drop constraint fk_event_lab_batch;
alter table event_fact drop column lab_batch_id;
drop table lab_batch cascade constraints;
drop table im_lab_batch cascade constraints;


CREATE TABLE lab_metric (
  lab_metric_id    NUMERIC(19) NOT NULL PRIMARY KEY,
  sample_name      VARCHAR(40) NOT NULL,
  lab_vessel_id    NUMERIC(19) NOT NULL,
  product_order_id NUMERIC(19),
  batch_name       VARCHAR(40),
  quant_type       VARCHAR2(255),
  quant_units      VARCHAR2(255),
  quant_value      NUMBER(19,2),
  run_name         VARCHAR2(255),
  run_date         DATE,
  etl_date         DATE NOT NULL
);

CREATE TABLE im_lab_metric (
  line_number      NUMERIC(9)  NOT NULL,
  etl_date         DATE        NOT NULL,
  is_delete        CHAR(1)     NOT NULL,
  lab_metric_id    NUMERIC(19) NOT NULL,
  sample_name      VARCHAR(40),
  lab_vessel_id    NUMERIC(19),
  product_order_id NUMERIC(19),
  batch_name       VARCHAR(40),
  quant_type       VARCHAR2(255),
  quant_units      VARCHAR2(255),
  quant_value      NUMBER(19,2),
  run_name         VARCHAR2(255),
  run_date         DATE
);

CREATE INDEX lab_metric_idx1 ON lab_metric (product_order_id, sample_name, batch_name);

ALTER TABLE lab_metric ADD CONSTRAINT fk_lab_metric_vessel_id FOREIGN KEY (lab_vessel_id)
REFERENCES lab_vessel (lab_vessel_id) ON DELETE CASCADE;

ALTER TABLE lab_metric ADD CONSTRAINT fk_lab_metric_pdo_id FOREIGN KEY (product_order_id)
REFERENCES product_order (product_order_id) ON DELETE CASCADE;
