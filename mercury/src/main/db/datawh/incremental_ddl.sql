
-------------------------------------------------------
-- For release 1.25
-------------------------------------------------------
/*
For debug purposes:
DROP SEQUENCE sequencing_sample_id_seq;
DROP TABLE sequencing_sample_fact CASCADE CONSTRAINTS;
DROP TABLE sequencing_run CASCADE CONSTRAINTS;
DROP TABLE im_sequencing_sample_fact CASCADE CONSTRAINTS;
DROP TABLE im_sequencing_run CASCADE CONSTRAINTS;
*/

CREATE TABLE sequencing_sample_fact (
  sequencing_sample_fact_id NUMERIC(19) NOT NULL PRIMARY KEY,
  flowcell_barcode VARCHAR2(255) NOT NULL,
  lane_name VARCHAR2(255) NOT NULL,
  molecular_indexing_scheme VARCHAR2(255) NOT NULL,
  sequencing_run_id NUMERIC(19) NOT NULL,
  product_order_id NUMERIC(19),
  sample_name VARCHAR2(40),
  research_project_id NUMERIC(19),
  etl_date DATE NOT NULL
);

CREATE TABLE sequencing_run (
  sequencing_run_id NUMERIC(19) NOT NULL PRIMARY KEY,
  run_name VARCHAR2(255),
  barcode VARCHAR2(255),
  registration_date DATE,
  instrument VARCHAR2(255),
  etl_date DATE NOT NULL
);

CREATE TABLE im_sequencing_sample_fact (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  sequencing_sample_fact_id NUMERIC(19),
  flowcell_barcode VARCHAR2(255),
  lane_name VARCHAR2(255),
  molecular_indexing_scheme VARCHAR2(255),
  sequencing_run_id NUMERIC(19),
  product_order_id NUMERIC(19),
  sample_name VARCHAR2(40),
  research_project_id NUMERIC(19)
);

CREATE TABLE im_sequencing_run (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  sequencing_run_id NUMERIC(19) NOT NULL,
  run_name VARCHAR2(255),
  barcode VARCHAR2(255),
  registration_date DATE,
  instrument VARCHAR2(255)
);

CREATE SEQUENCE sequencing_sample_id_seq start with 1;
 
ALTER TABLE sequencing_sample_fact ADD CONSTRAINT fk_seq_sample_seqrun_id FOREIGN KEY (sequencing_run_id)
  REFERENCES sequencing_run(sequencing_run_id) ON DELETE CASCADE;

ALTER TABLE sequencing_sample_fact ADD CONSTRAINT fk_seq_sample_pdo_id FOREIGN KEY (product_order_id)
  REFERENCES product_order(product_order_id) ON DELETE CASCADE;

ALTER TABLE sequencing_sample_fact ADD CONSTRAINT fk_seq_sample_rpid FOREIGN KEY (research_project_id)
  REFERENCES research_project(research_project_id) ON DELETE CASCADE;
 
CREATE UNIQUE INDEX seq_sample_fact_idx1 ON sequencing_sample_fact(flowcell_barcode, lane_name, molecular_indexing_scheme);
CREATE INDEX seq_sample_fact_idx2 ON sequencing_sample_fact(product_order_id, sample_name);
CREATE INDEX seq_sample_fact_idx3 ON sequencing_sample_fact(sequencing_run_id);
-- add this for efficient re-export of existing entity ids
CREATE INDEX event_fact_idx3 ON event_fact(lab_event_id);

ALTER TABLE product ADD (primary_price_item_id number(19,0));

ALTER TABLE product ADD CONSTRAINT fk_product_price_item_id FOREIGN KEY (primary_price_item_id)
REFERENCES price_item(price_item_id) ON DELETE CASCADE;
