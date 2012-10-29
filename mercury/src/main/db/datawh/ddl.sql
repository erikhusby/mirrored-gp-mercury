
DROP TABLE product_order_po_sample;
DROP TABLE research_project_po;
DROP TABLE po_billable_item;
DROP TABLE po_sample_billing_status;
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
DROP TABLE product_add_on;
DROP TABLE product;


CREATE TABLE product (
  product_id NUMERIC(19) PRIMARY KEY NOT NULL,
  product_name VARCHAR2(80) NOT NULL,
  part_number VARCHAR2(80) NOT NULL,
  availability_date DATE NOT NULL,
  discontinued_date DATE,
  expected_cycle_time_sec NUMERIC(19),
  guaranteed_cycle_time_sec NUMERIC(19),
  samples_per_week NUMERIC(19),
  is_top_level_product CHAR(1) CHECK (is_top_level_product IN ('T','F')),
  workflow_name VARCHAR2(80),
  etl_date DATE NOT NULL
);

CREATE TABLE product_add_on (
  product_id NUMERIC(19) NOT NULL,
  add_on_product_id NUMERIC(19) NOT NULL,
  etl_date DATE NOT NULL,
  CONSTRAINT fk_product_add_on_product FOREIGN KEY (product_id)
    REFERENCES product(product_id),
  CONSTRAINT fk_product_add_on_add_on FOREIGN KEY (add_on_product_id)
    REFERENCES product(product_id),
  PRIMARY KEY (product_id, add_on_product_id)
);

CREATE TABLE price_item (
  price_item_id NUMERIC(19) PRIMARY KEY NOT NULL,
  platform VARCHAR2(80) NOT NULL,
  category VARCHAR2(80) NOT NULL,
  price_item_name VARCHAR2(80) NOT NULL,
  quote_server_id VARCHAR2(80) NOT NULL,
  price NUMERIC(10,4),
  units VARCHAR2(19),
  etl_date DATE NOT NULL
);


CREATE TABLE research_project (
  research_project_id NUMERIC(19) PRIMARY KEY NOT NULL,
  current_status VARCHAR2(19) NOT NULL,
  created_date DATE NOT NULL,
  title VARCHAR2(80) NOT NULL,
  irb_not_engaged CHAR(1) CHECK (irb_not_engaged IN ('T','F')),
  jira_ticket_key VARCHAR2(80) NOT NULL,
  etl_date DATE NOT NULL
);

CREATE TABLE research_project_status (
  research_project_id NUMERIC(19) NOT NULL,
  status_date DATE NOT NULL,
  status VARCHAR2(19) NOT NULL,
  etl_date DATE NOT NULL,
  CONSTRAINT fk_rp_status_rpid FOREIGN KEY (research_project_id)
    REFERENCES research_project(research_project_id),
  PRIMARY KEY (research_project_id, status_date)
);

CREATE TABLE research_project_person (
  research_project_person_id NUMERIC(19) PRIMARY KEY NOT NULL,
  research_project_id NUMERIC(19) NOT NULL,
  project_role VARCHAR2(19) NOT NULL,
  person_id NUMERIC(19)  NOT NULL,
  first_name VARCHAR2(80),
  last_name VARCHAR2(80),
  username VARCHAR2(80),
  etl_date DATE NOT NULL,
  CONSTRAINT fk_rp_person_rpid FOREIGN KEY (research_project_id)
    REFERENCES research_project(research_project_id)
);

CREATE TABLE research_project_funding (
  research_project_id NUMERIC(19) NOT NULL,
  research_project_funding_id NUMERIC(19) NOT NULL,
  etl_date DATE NOT NULL,
  PRIMARY KEY (research_project_id, research_project_funding_id),
  CONSTRAINT fk_rp_funding_rpid FOREIGN KEY (research_project_id)
    REFERENCES research_project(research_project_id)
);

CREATE TABLE research_project_cohort (
  research_project_id NUMERIC(19) NOT NULL,
  research_project_cohort_id NUMERIC(19) NOT NULL,
  etl_date DATE NOT NULL,
  PRIMARY KEY (research_project_id, research_project_cohort_id),
  CONSTRAINT fk_rp_cohort_rpid FOREIGN KEY (research_project_id)
    REFERENCES research_project(research_project_id)
);

CREATE TABLE research_project_irb (
  research_project_id NUMERIC(19) NOT NULL,
  research_project_irb_id NUMERIC(19) NOT NULL,
  research_project_irb VARCHAR2(80),
  research_project_irb_type VARCHAR2(19) NOT NULL,
  etl_date DATE NOT NULL,
  PRIMARY KEY (research_project_id, research_project_irb_id),
  CONSTRAINT fk_rp_irb_rpid FOREIGN KEY (research_project_id)
    REFERENCES research_project(research_project_id)
);

CREATE TABLE product_order (
  product_order_id NUMERIC(19) PRIMARY KEY NOT NULL,
  product_id NUMERIC(19) NOT NULL,
  status VARCHAR2(19) NOT NULL,
  created_date DATE NOT NULL,
  modified_date DATE,
  title VARCHAR2(80),
  quote_id VARCHAR2(80),
  jira_ticket_key VARCHAR2(80),
  etl_date DATE NOT NULL,
  CONSTRAINT fk_po_productid FOREIGN KEY (product_id)
    REFERENCES product(product_id)
);

CREATE TABLE product_order_status (
  product_order_id NUMERIC(19) NOT NULL,
  status_date DATE NOT NULL,
  status VARCHAR2(19) NOT NULL,
  etl_date DATE NOT NULL,
  CONSTRAINT fk_po_status_poid FOREIGN KEY (product_order_id)
    REFERENCES product_order(product_order_id),
  PRIMARY KEY (product_order_id, status_date)
);

CREATE TABLE product_order_sample (
  product_order_sample_id NUMERIC(19) PRIMARY KEY NOT NULL,
  sample_name VARCHAR2(80),
  billing_status VARCHAR2(19) NOT NULL,
  etl_date DATE NOT NULL
);

CREATE TABLE po_sample_billing_status (
  product_order_sample_id NUMERIC(19) NOT NULL,
  status_date DATE NOT NULL,
  billing_status VARCHAR2(19) NOT NULL,
  etl_date DATE NOT NULL,
  CONSTRAINT fk_po_sample_b_s_po_sid FOREIGN KEY (product_order_sample_id)
    REFERENCES product_order_sample(product_order_sample_id),
  PRIMARY KEY (product_order_sample_id, status_date)
);

CREATE TABLE po_billable_item (
  product_order_sample_id NUMERIC(19) NOT NULL,
  price_item_id NUMERIC(19) NOT NULL,
  item_count NUMERIC(19) NOT NULL,
  etl_date DATE NOT NULL,
  PRIMARY KEY (product_order_sample_id, price_item_id),
  CONSTRAINT fk_po_billable_item_po_sid FOREIGN KEY (product_order_sample_id)
    REFERENCES product_order_sample(product_order_sample_id),
  CONSTRAINT fk_po_billable_item_piid FOREIGN KEY (price_item_id)
    REFERENCES  price_item(price_item_id)
);


--  Bridge Tables

CREATE TABLE research_project_po (
  research_project_id NUMERIC(19) NOT NULL,
  product_order_id NUMERIC(19) NOT NULL,
  etl_date DATE NOT NULL,
  CONSTRAINT fk_rp_po_rpid FOREIGN KEY (research_project_id)
    REFERENCES research_project(research_project_id),
  CONSTRAINT fk_rp_po_poid FOREIGN KEY (product_order_id)
    REFERENCES product_order(product_order_id),
  PRIMARY KEY (research_project_id, product_order_id)
);

CREATE TABLE product_order_po_sample (
  product_order_id NUMERIC(19) NOT NULL,
  product_order_sample_id NUMERIC(19) NOT NULL,
  etl_date DATE NOT NULL,
  CONSTRAINT fk_po_po_sample_poid FOREIGN KEY (product_order_id)
    REFERENCES product_order(product_order_id),
  CONSTRAINT fk_po_po_sample_sid FOREIGN KEY (product_order_sample_id)
    REFERENCES product_order_sample(product_order_sample_id),
  PRIMARY KEY (product_order_id, product_order_sample_id)
);


-- Import tables
DROP TABLE im_product_order_po_sample;
DROP TABLE im_research_project_po;
DROP TABLE im_po_billable_item;
DROP TABLE im_product_order_sample;
DROP TABLE im_product_order_status;
DROP TABLE im_product_order;
DROP TABLE im_po_sample_billing_status;
DROP TABLE im_research_project_irb;
DROP TABLE im_research_project_cohort;
DROP TABLE im_research_project_funding;
DROP TABLE im_research_project_person;
DROP TABLE im_research_project;
DROP TABLE im_research_project_status;
DROP TABLE im_price_item;
DROP TABLE im_product_add_on;
DROP TABLE im_product;


CREATE TABLE im_product (
  product_id NUMERIC(19) NOT NULL,
  product_name VARCHAR2(80) NOT NULL,
  part_number VARCHAR2(80) NOT NULL,
  availability_date DATE NOT NULL,
  discontinued_date DATE,
  expected_cycle_time_sec NUMERIC(19),
  guaranteed_cycle_time_sec NUMERIC(19),
  samples_per_week NUMERIC(19),
  is_top_level_product CHAR(1) NOT NULL,
  workflow_name VARCHAR2(80),
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL
);

CREATE TABLE im_product_add_on (
  product_id NUMERIC(19) NOT NULL,
  add_on_product_id NUMERIC(19) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL
);

CREATE TABLE im_price_item (
  price_item_id NUMERIC(19) NOT NULL,
  platform VARCHAR2(80) NOT NULL,
  category VARCHAR2(80) NOT NULL,
  price_item_name VARCHAR2(80) NOT NULL,
  quote_server_id VARCHAR2(80) NOT NULL,
  price NUMERIC(10,4),
  units VARCHAR2(19),
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL
);


CREATE TABLE im_research_project (
  research_project_id NUMERIC(19) NOT NULL,
  current_status VARCHAR2(19) NOT NULL,
  created_date DATE NOT NULL,
  title VARCHAR2(80) NOT NULL,
  irb_not_engaged CHAR(1) CHECK (irb_not_engaged IN ('T','F')),
  jira_ticket_key VARCHAR2(80) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL
);

CREATE TABLE im_research_project_status (
  research_project_id NUMERIC(19) NOT NULL,
  status_date DATE NOT NULL,
  status VARCHAR2(19) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL
);

CREATE TABLE im_research_project_person (
  research_project_person_id NUMERIC(19) NOT NULL,
  research_project_id NUMERIC(19) NOT NULL,
  project_role VARCHAR2(19) NOT NULL,
  person_id NUMERIC(19)  NOT NULL,
  first_name VARCHAR2(80),
  last_name VARCHAR2(80),
  username VARCHAR2(80),
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL
);

CREATE TABLE im_research_project_funding (
  research_project_id NUMERIC(19) NOT NULL,
  research_project_funding_id NUMERIC(19) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL
);

CREATE TABLE im_research_project_cohort (
  research_project_id NUMERIC(19) NOT NULL,
  research_project_cohort_id NUMERIC(19) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL
);

CREATE TABLE im_research_project_irb (
  research_project_id NUMERIC(19) NOT NULL,
  research_project_irb_id NUMERIC(19) NOT NULL,
  research_project_irb VARCHAR2(80),
  research_project_irb_type VARCHAR2(19) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL
);

CREATE TABLE im_product_order (
  product_order_id NUMERIC(19) NOT NULL,
  product_id NUMERIC(19) NOT NULL,
  status VARCHAR2(19) NOT NULL,
  created_date DATE NOT NULL,
  modified_date DATE,
  title VARCHAR2(80),
  quote_id VARCHAR2(80),
  jira_ticket_key VARCHAR2(80),
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL
);

CREATE TABLE im_product_order_status (
  product_order_id NUMERIC(19) NOT NULL,
  status_date DATE NOT NULL,
  status VARCHAR2(19) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL
);

CREATE TABLE im_po_sample_billing_status (
  product_order_sample_id NUMERIC(19) NOT NULL,
  status_date DATE NOT NULL,
  billing_status VARCHAR2(19) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL
);


CREATE TABLE im_product_order_sample (
  product_order_sample_id NUMERIC(19) NOT NULL,
  sample_name VARCHAR2(80),
  billing_status VARCHAR2(19) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL
);

CREATE TABLE im_po_billable_item (
  product_order_sample_id NUMERIC(19) NOT NULL,
  price_item_id NUMERIC(19) NOT NULL,
  item_count NUMERIC(19) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL
);

CREATE TABLE im_research_project_po (
  research_project_id NUMERIC(19) NOT NULL,
  product_order_id NUMERIC(19) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL
);

CREATE TABLE im_product_order_po_sample (
  product_order_id NUMERIC(19) NOT NULL,
  product_order_sample_id NUMERIC(19) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL
);

COMMIT;
