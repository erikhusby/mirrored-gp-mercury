-------------------------------------------------------
-- For release 1.16
-------------------------------------------------------
CREATE TABLE lab_vessel (
  lab_vessel_id NUMERIC(19) NOT NULL PRIMARY KEY,
  label VARCHAR2(40) NOT NULL,
  lab_vessel_type VARCHAR2(40) NOT NULL,
  etl_date DATE NOT NULL
);

CREATE TABLE lab_batch (
  lab_batch_id NUMERIC(19) NOT NULL PRIMARY KEY,
  batch_name VARCHAR2(40) NOT NULL,
  is_active CHAR(1) DEFAULT 'T' NOT NULL CHECK (is_active IN ('T','F')),
  created_on DATE,
  due_date DATE,
  etl_date DATE NOT NULL
);

CREATE TABLE workflow_config (
  workflow_config_id NUMERIC(19) NOT NULL PRIMARY KEY,
  effective_date DATE NOT NULL,
  workflow_name VARCHAR2(255) NOT NULL,
  workflow_version VARCHAR2(40) NOT NULL,
  process_name VARCHAR2(255) NOT NULL,
  process_version VARCHAR2(40) NOT NULL,
  step_name VARCHAR2(255) NOT NULL,
  event_name VARCHAR2(255) NOT NULL,
  etl_date DATE NOT NULL
);

CREATE TABLE event_fact (
  event_fact_id NUMERIC(28) NOT NULL PRIMARY KEY,
  lab_event_id NUMERIC(19) NOT NULL,
  event_name VARCHAR2(255) NOT NULL,
  workflow_config_id NUMERIC(19),
  product_order_id NUMERIC(19),
  sample_key  VARCHAR(40),
  lab_batch_id NUMERIC(19),
  station_name VARCHAR2(255),
  lab_vessel_id NUMERIC(19),
  event_date DATE NOT NULL,
  etl_date DATE NOT NULL
);

CREATE TABLE im_lab_vessel (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  lab_vessel_id NUMERIC(19) NOT NULL,
  label VARCHAR2(40),
  lab_vessel_type VARCHAR2(40)
);

CREATE TABLE im_lab_batch (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  lab_batch_id NUMERIC(19) NOT NULL,
  batch_name VARCHAR2(40),
  is_active CHAR(1),
  created_on DATE,
  due_date DATE
);

CREATE TABLE im_workflow_config (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  workflow_config_id NUMERIC(19) NOT NULL,
  effective_date DATE,
  workflow_name VARCHAR2(255),
  workflow_version VARCHAR2(40),
  process_name VARCHAR2(255),
  process_version VARCHAR2(40),
  step_name VARCHAR2(255),
  event_name VARCHAR2(255)
);

CREATE TABLE im_event_fact (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  lab_event_id NUMERIC(19) NOT NULL,
  event_name VARCHAR2(255),
  workflow_config_id NUMERIC(19),
  product_order_id NUMERIC(19),
  sample_key  VARCHAR(40),
  lab_batch_id NUMERIC(19),
  station_name VARCHAR2(255),
  lab_vessel_id NUMERIC(19),
  event_date DATE,
  event_fact_id NUMERIC(19)
);

CREATE SEQUENCE event_fact_id_seq start with 1;

ALTER TABLE event_fact ADD CONSTRAINT fk_event_lab_vessel FOREIGN KEY (lab_vessel_id)
  REFERENCES lab_vessel(lab_vessel_id) ON DELETE CASCADE;

ALTER TABLE event_fact ADD CONSTRAINT fk_event_lab_batch FOREIGN KEY (lab_batch_id)
  REFERENCES lab_batch(lab_batch_id) ON DELETE CASCADE;

ALTER TABLE event_fact ADD CONSTRAINT fk_event_pdo FOREIGN KEY (product_order_id)
  REFERENCES product_order(product_order_id) ON DELETE CASCADE;

ALTER TABLE event_fact ADD CONSTRAINT fk_event_workflow_config FOREIGN KEY (workflow_config_id)
  REFERENCES workflow_config(workflow_config_id) ON DELETE CASCADE;

CREATE INDEX event_fact_idx1 ON event_fact(event_date);
CREATE INDEX event_fact_idx2 ON event_fact(product_order_id);
-------------------------------------------------------
-- end Release 1.16
-------------------------------------------------------
