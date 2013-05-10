DROP INDEX research_project_status_idx1;
DROP INDEX research_project_person_idx1;
DROP INDEX research_project_fund_idx1;
DROP INDEX research_project_cohort_idx1;
DROP INDEX research_project_irb_idx1;
DROP INDEX product_order_status_idx1;
DROP INDEX product_order_idx1;
DROP INDEX product_order_idx2;
DROP INDEX product_order_sample_idx1;
DROP INDEX pdo_sample_status_idx1;
DROP INDEX pdo_add_on_idx1;
DROP INDEX pdo_add_on_idx2;
DROP INDEX event_fact_idx1;
DROP INDEX event_fact_idx2;

ALTER TABLE research_project_status DROP CONSTRAINT fk_rp_status_rpid;
ALTER TABLE research_project_person DROP CONSTRAINT fk_rp_person_rpid;
ALTER TABLE research_project_funding DROP CONSTRAINT fk_rp_funding_rpid;
ALTER TABLE research_project_cohort DROP CONSTRAINT fk_rp_cohort_rpid;
ALTER TABLE research_project_irb DROP CONSTRAINT fk_rp_irb_rpid;
ALTER TABLE product_order DROP CONSTRAINT fk_po_rpid;
ALTER TABLE product_order DROP CONSTRAINT fk_po_productid;
ALTER TABLE product_order_status DROP CONSTRAINT fk_po_status_poid;
ALTER TABLE product_order_sample DROP CONSTRAINT fk_pos_poid;
ALTER TABLE product_order_sample_status DROP CONSTRAINT fk_po_sample_b_s_po_sid;
ALTER TABLE product_order_add_on DROP CONSTRAINT fk_po_add_on_prodid;
ALTER TABLE product_order_add_on DROP CONSTRAINT fk_po_add_on_poid;
ALTER TABLE event_fact DROP CONSTRAINT fk_event_lab_batch;
ALTER TABLE event_fact DROP CONSTRAINT fk_event_lab_vessel;
ALTER TABLE event_fact DROP CONSTRAINT fk_event_workflow;
ALTER TABLE event_fact DROP CONSTRAINT fk_event_process;
ALTER TABLE event_fact DROP CONSTRAINT fk_event_pdo;

DROP SEQUENCE event_fact_id_seq;

DROP TABLE product_order_add_on;
DROP TABLE product_order_sample_status;
DROP TABLE product_order_sample;
DROP TABLE product_order_status;
DROP TABLE product_order;
DROP TABLE research_project_irb;
DROP TABLE research_project_cohort;
DROP TABLE research_project_funding;
DROP TABLE research_project_person;
DROP TABLE research_project_status;
DROP TABLE research_project;
DROP TABLE price_item;
DROP TABLE product;
DROP TABLE lab_batch;
DROP TABLE lab_vessel;
DROP TABLE event_fact;
DROP TABLE workflow;
DROP TABLE workflow_process;

DROP TABLE im_product_order_add_on;
DROP TABLE im_product_order_sample_bill;
DROP TABLE im_product_order_sample_risk;
DROP TABLE im_product_order_sample_stat;
DROP TABLE im_product_order_sample;
DROP TABLE im_product_order_status;
DROP TABLE im_product_order;
DROP TABLE im_research_project_irb;
DROP TABLE im_research_project_cohort;
DROP TABLE im_research_project_funding;
DROP TABLE im_research_project_person;
DROP TABLE im_research_project;
DROP TABLE im_research_project_status;
DROP TABLE im_price_item;
DROP TABLE im_product;
DROP TABLE im_lab_vessel;
DROP TABLE im_lab_batch;
DROP TABLE im_event_fact;
DROP TABLE im_workflow;
DROP TABLE im_workflow_process;



CREATE TABLE product (
  product_id NUMERIC(19) PRIMARY KEY NOT NULL,
  product_name VARCHAR2(255) NOT NULL,
  part_number VARCHAR2(255) NOT NULL,
  availability_date DATE NOT NULL,
  discontinued_date DATE,
  expected_cycle_time_sec NUMERIC(19),
  guaranteed_cycle_time_sec NUMERIC(19),
  samples_per_week NUMERIC(19),
  is_top_level_product CHAR(1) CHECK (is_top_level_product IN ('T','F')),
  workflow_name VARCHAR2(255),
  product_family_name VARCHAR2(255),
  etl_date DATE NOT NULL
);

CREATE TABLE price_item (
  price_item_id NUMERIC(19) PRIMARY KEY NOT NULL,
  platform VARCHAR2(255) NOT NULL,
  category VARCHAR2(255) NOT NULL,
  price_item_name VARCHAR2(255) NOT NULL,
  quote_server_id VARCHAR2(255) NOT NULL,
  price NUMERIC(10,4),
  units VARCHAR2(80),
  etl_date DATE NOT NULL
);


CREATE TABLE research_project (
  research_project_id NUMERIC(19) PRIMARY KEY NOT NULL,
  current_status VARCHAR2(40) NOT NULL,
  created_date DATE NOT NULL,
  title VARCHAR2(255) NOT NULL,
  irb_not_engaged CHAR(1) CHECK (irb_not_engaged IN ('T','F')),
  jira_ticket_key VARCHAR2(255),
  etl_date DATE NOT NULL
);

CREATE TABLE research_project_status (
  research_project_id NUMERIC(19) NOT NULL,
  status_date DATE NOT NULL,
  status VARCHAR2(40) NOT NULL,
  etl_date DATE NOT NULL,
  PRIMARY KEY (research_project_id, status_date)
);

CREATE TABLE research_project_person (
  research_project_person_id NUMERIC(19) PRIMARY KEY NOT NULL,
  research_project_id NUMERIC(19) NOT NULL,
  project_role VARCHAR2(80) NOT NULL,
  person_id NUMERIC(19)  NOT NULL,
  first_name VARCHAR2(255),
  last_name VARCHAR2(255),
  username VARCHAR2(255),
  etl_date DATE NOT NULL
);

CREATE TABLE research_project_funding (
  research_project_funding_id NUMERIC(19) NOT NULL PRIMARY KEY,
  research_project_id NUMERIC(19) NOT NULL,
  funding_id VARCHAR2(255) NOT NULL,
  etl_date DATE NOT NULL
);

CREATE TABLE research_project_cohort (
  research_project_cohort_id NUMERIC(19) NOT NULL PRIMARY KEY,
  research_project_id NUMERIC(19) NOT NULL,
  etl_date DATE NOT NULL
);

CREATE TABLE research_project_irb (
  research_project_irb_id NUMERIC(19) NOT NULL PRIMARY KEY,
  research_project_id NUMERIC(19) NOT NULL,
  research_project_irb VARCHAR2(255) NOT NULL,
  research_project_irb_type VARCHAR2(255) NOT NULL,
  etl_date DATE NOT NULL
);

CREATE TABLE product_order (
  product_order_id NUMERIC(19) PRIMARY KEY NOT NULL,
  research_project_id NUMERIC(19),
  product_id NUMERIC(19),
  status VARCHAR2(40) NOT NULL,
  created_date DATE,
  modified_date DATE,
  title VARCHAR2(255),
  quote_id VARCHAR2(255),
  jira_ticket_key VARCHAR2(255),
  owner VARCHAR2(40),
  placed_date DATE,
  etl_date DATE NOT NULL
);

CREATE TABLE product_order_status (
  product_order_id NUMERIC(19) NOT NULL,
  status_date DATE NOT NULL,
  status VARCHAR2(40) NOT NULL,
  etl_date DATE NOT NULL,
  PRIMARY KEY (product_order_id, status_date)
);

CREATE TABLE product_order_sample (
  product_order_sample_id NUMERIC(19) PRIMARY KEY NOT NULL,
  product_order_id NUMERIC(19) NOT NULL,
  sample_name VARCHAR2(255),
  delivery_status VARCHAR2(40) NOT NULL,
  sample_position NUMERIC(19) NOT NULL,
  on_risk CHAR(1) DEFAULT 'F' NOT NULL CHECK (on_risk IN ('T','F')),
  is_billed CHAR(1) DEFAULT 'F' NOT NULL CHECK (is_billed IN ('T','F')),
  is_abandoned CHAR(1) generated always as (case when delivery_status = 'ABANDONED' then 'T' else 'F' end) virtual,
  etl_date DATE NOT NULL
);

CREATE TABLE product_order_sample_status (
  product_order_sample_id NUMERIC(19) NOT NULL,
  status_date DATE NOT NULL,
  delivery_status VARCHAR2(40) NOT NULL,
  etl_date DATE NOT NULL,
  PRIMARY KEY (product_order_sample_id, status_date)
);

CREATE TABLE product_order_add_on (
  product_order_add_on_id NUMERIC(19) NOT NULL PRIMARY KEY,
  product_order_id NUMERIC(19) NOT NULL,
  product_id NUMERIC(19) NOT NULL,
  etl_date DATE NOT NULL
);

CREATE TABLE lab_vessel (
  lab_vessel_id NUMERIC(19) NOT NULL PRIMARY KEY,
  label VARCHAR2(40) NOT NULL,
  lab_vessel_type VARCHAR2(40) NOT NULL,
  etl_date DATE NOT NULL
);

CREATE TABLE lab_batch (
  lab_batch_id NUMERIC(19) NOT NULL PRIMARY KEY,
  batch_name VARCHAR2(40) NOT NULL,
  etl_date DATE NOT NULL
);

CREATE TABLE workflow (
  workflow_id NUMERIC(19) NOT NULL PRIMARY KEY,
  workflow_name VARCHAR2(255) NOT NULL,
  workflow_version VARCHAR2(40) NOT NULL,
  etl_date DATE NOT NULL
);

CREATE TABLE workflow_process (
  process_id NUMERIC(19) NOT NULL PRIMARY KEY,
  process_name VARCHAR2(255) NOT NULL,
  process_version VARCHAR2(40) NOT NULL,
  step_name VARCHAR2(255) NOT NULL,
  event_name VARCHAR2(255) NOT NULL,
  etl_date DATE NOT NULL
);

CREATE TABLE event_fact (
  event_fact_id NUMERIC(28) NOT NULL PRIMARY KEY,
  lab_event_id NUMERIC(19) NOT NULL,
  workflow_id NUMERIC(19),
  process_id NUMERIC(19),
  product_order_id NUMERIC(19),
  sample_name VARCHAR(40),
  lab_batch_id NUMERIC(19),
  station_name VARCHAR2(255),
  lab_vessel_id NUMERIC(19),
  event_date DATE NOT NULL,
  etl_date DATE NOT NULL
);


-- The import tables

CREATE TABLE im_product (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  product_id NUMERIC(19) NOT NULL,
  product_name VARCHAR2(255),
  part_number VARCHAR2(255),
  availability_date DATE,
  discontinued_date DATE,
  expected_cycle_time_sec NUMERIC(19),
  guaranteed_cycle_time_sec NUMERIC(19),
  samples_per_week NUMERIC(19),
  is_top_level_product CHAR(1),
  workflow_name VARCHAR2(255),
  product_family_name VARCHAR2(255)
);

CREATE TABLE im_price_item (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  price_item_id NUMERIC(19) NOT NULL,
  platform VARCHAR2(255),
  category VARCHAR2(255),
  price_item_name VARCHAR2(255),
  quote_server_id VARCHAR2(255),
  price NUMERIC(10,4),
  units VARCHAR2(80)
);


CREATE TABLE im_research_project (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  research_project_id NUMERIC(19) NOT NULL,
  current_status VARCHAR2(40),
  created_date DATE,
  title VARCHAR2(255),
  irb_not_engaged CHAR(1) CHECK (irb_not_engaged IN ('T','F')),
  jira_ticket_key VARCHAR2(255)
);

CREATE TABLE im_research_project_status (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  research_project_id NUMERIC(19) NOT NULL,
  status_date DATE,
  status VARCHAR2(40)
);

CREATE TABLE im_research_project_person (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  research_project_person_id NUMERIC(19) NOT NULL,
  research_project_id NUMERIC(19),
  project_role VARCHAR2(80),
  person_id NUMERIC(19) ,
  first_name VARCHAR2(255),
  last_name VARCHAR2(255),
  username VARCHAR2(255)
);

CREATE TABLE im_research_project_funding (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  research_project_funding_id NUMERIC(19) NOT NULL,
  research_project_id NUMERIC(19),
  funding_id VARCHAR2(255)
);

CREATE TABLE im_research_project_cohort (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  research_project_cohort_id NUMERIC(19) NOT NULL,
  research_project_id NUMERIC(19)
);

CREATE TABLE im_research_project_irb (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  research_project_irb_id NUMERIC(19) NOT NULL,
  research_project_id NUMERIC(19),
  research_project_irb VARCHAR2(255),
  research_project_irb_type VARCHAR2(255)
);

CREATE TABLE im_product_order (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  product_order_id NUMERIC(19) NOT NULL,
  research_project_id NUMERIC(19),
  product_id NUMERIC(19),
  status VARCHAR2(40),
  created_date DATE,
  modified_date DATE,
  title VARCHAR2(255),
  quote_id VARCHAR2(255),
  jira_ticket_key VARCHAR2(255),
  owner VARCHAR2(40),
  placed_date DATE
);

CREATE TABLE im_product_order_status (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  product_order_id NUMERIC(19) NOT NULL,
  status_date DATE,
  status VARCHAR2(40)
);

CREATE TABLE im_product_order_sample_stat (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  product_order_sample_id NUMERIC(19) NOT NULL,
  status_date DATE,
  delivery_status VARCHAR2(40)
);


CREATE TABLE im_product_order_sample (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  product_order_sample_id NUMERIC(19) NOT NULL,
  product_order_id NUMERIC(19),
  sample_name VARCHAR2(255),
  delivery_status VARCHAR2(40),
  sample_position NUMERIC(19)
);

CREATE TABLE im_product_order_add_on (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  product_order_add_on_id NUMERIC(19) NOT NULL,
  product_order_id NUMERIC(19),
  product_id NUMERIC(19)
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
  batch_name VARCHAR2(40)
);

CREATE TABLE im_workflow (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  workflow_id NUMERIC(19) NOT NULL,
  workflow_name VARCHAR2(255),
  workflow_version VARCHAR2(40)
);

CREATE TABLE im_workflow_process (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  process_id NUMERIC(19) NOT NULL,
  process_name VARCHAR2(255),
  process_version VARCHAR2(40),
  step_name VARCHAR2(255),
  event_name VARCHAR2(255)
);

CREATE TABLE im_event_fact (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  lab_event_id NUMERIC(19),
  workflow_id NUMERIC(19),
  process_id NUMERIC(19),
  product_order_id NUMERIC(19),
  sample_name  VARCHAR(40),
  lab_batch_id NUMERIC(19),
  station_name VARCHAR2(255),
  lab_vessel_id NUMERIC(19),
  event_date DATE,
  event_fact_id NUMERIC(28) --this gets populated by merge_import.sql
);

CREATE TABLE im_product_order_sample_risk (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  product_order_sample_id NUMERIC(19) NOT NULL,
  on_risk CHAR(1)
);

CREATE TABLE im_product_order_sample_bill (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  product_order_sample_id NUMERIC(19) NOT NULL,
  is_billed CHAR(1)
);

CREATE SEQUENCE event_fact_id_seq start with 1;

ALTER TABLE research_project_status ADD CONSTRAINT fk_rp_status_rpid FOREIGN KEY (research_project_id)
  REFERENCES research_project(research_project_id) ON DELETE CASCADE;

ALTER TABLE research_project_person ADD CONSTRAINT fk_rp_person_rpid FOREIGN KEY (research_project_id)
  REFERENCES research_project(research_project_id) ON DELETE CASCADE;

ALTER TABLE research_project_funding ADD CONSTRAINT fk_rp_funding_rpid FOREIGN KEY (research_project_id)
  REFERENCES research_project(research_project_id) ON DELETE CASCADE;

ALTER TABLE research_project_cohort ADD CONSTRAINT fk_rp_cohort_rpid FOREIGN KEY (research_project_id)
  REFERENCES research_project(research_project_id) ON DELETE CASCADE;

ALTER TABLE research_project_irb ADD CONSTRAINT fk_rp_irb_rpid FOREIGN KEY (research_project_id)
  REFERENCES research_project(research_project_id) ON DELETE CASCADE;

ALTER TABLE product_order ADD CONSTRAINT fk_po_rpid FOREIGN KEY (research_project_id)
  REFERENCES research_project(research_project_id)  ON DELETE CASCADE;

ALTER TABLE product_order ADD CONSTRAINT fk_po_productid FOREIGN KEY (product_id)
  REFERENCES product(product_id) ON DELETE CASCADE;

ALTER TABLE product_order_status ADD CONSTRAINT fk_po_status_poid FOREIGN KEY (product_order_id)
  REFERENCES product_order(product_order_id) ON DELETE CASCADE;

ALTER TABLE product_order_sample ADD CONSTRAINT fk_pos_poid FOREIGN KEY (product_order_id)
  REFERENCES product_order(product_order_id) ON DELETE CASCADE;

ALTER TABLE product_order_sample_status ADD CONSTRAINT fk_po_sample_b_s_po_sid FOREIGN KEY (product_order_sample_id)
  REFERENCES product_order_sample(product_order_sample_id) ON DELETE CASCADE;

ALTER TABLE product_order_add_on ADD CONSTRAINT fk_po_add_on_prodid FOREIGN KEY (product_id)
  REFERENCES product(product_id) ON DELETE CASCADE;

ALTER TABLE product_order_add_on ADD CONSTRAINT fk_po_add_on_poid FOREIGN KEY (product_order_id)
  REFERENCES product_order(product_order_id) ON DELETE CASCADE;

ALTER TABLE event_fact ADD CONSTRAINT fk_event_lab_vessel FOREIGN KEY (lab_vessel_id)
  REFERENCES lab_vessel(lab_vessel_id) ON DELETE CASCADE;

ALTER TABLE event_fact ADD CONSTRAINT fk_event_lab_batch FOREIGN KEY (lab_batch_id)
  REFERENCES lab_batch(lab_batch_id) ON DELETE CASCADE;

ALTER TABLE event_fact ADD CONSTRAINT fk_event_pdo FOREIGN KEY (product_order_id)
  REFERENCES product_order(product_order_id) ON DELETE CASCADE;

ALTER TABLE event_fact ADD CONSTRAINT fk_event_workflow FOREIGN KEY (workflow_id)
  REFERENCES workflow(workflow_id) ON DELETE CASCADE;

ALTER TABLE event_fact ADD CONSTRAINT fk_event_process FOREIGN KEY (process_id)
  REFERENCES workflow_process(process_id) ON DELETE CASCADE;


CREATE INDEX research_project_status_idx1 ON research_project_status(research_project_id);
CREATE INDEX research_project_person_idx1 ON research_project_person(research_project_id);
CREATE INDEX research_project_fund_idx1 ON research_project_funding(research_project_id);
CREATE INDEX research_project_cohort_idx1 ON research_project_cohort(research_project_id);
CREATE INDEX research_project_irb_idx1 ON research_project_irb(research_project_id);
CREATE INDEX product_order_idx1 ON product_order(research_project_id);
CREATE INDEX product_order_idx2 ON product_order(product_id);
CREATE INDEX product_order_status_idx1 ON product_order_status(product_order_id);
CREATE UNIQUE INDEX product_order_sample_idx1 ON product_order_sample(product_order_id, sample_name, sample_position);
CREATE INDEX pdo_sample_status_idx1 ON product_order_sample_status(product_order_sample_id);
CREATE INDEX pdo_add_on_idx1 ON product_order_add_on(product_order_id);
CREATE INDEX pdo_add_on_idx2 ON product_order_add_on(product_id);
CREATE INDEX event_fact_idx1 ON event_fact(event_date);
CREATE INDEX event_fact_idx2 ON event_fact(product_order_id);


COMMIT;
