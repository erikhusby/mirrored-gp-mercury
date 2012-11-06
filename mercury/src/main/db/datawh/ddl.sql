
DROP TABLE billable_item;
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
  current_status VARCHAR2(19) NOT NULL,
  created_date DATE NOT NULL,
  title VARCHAR2(255) NOT NULL,
  irb_not_engaged CHAR(1) CHECK (irb_not_engaged IN ('T','F')),
  jira_ticket_key VARCHAR2(255),
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
  project_role VARCHAR2(80) NOT NULL,
  person_id NUMERIC(19)  NOT NULL,
  first_name VARCHAR2(255),
  last_name VARCHAR2(255),
  username VARCHAR2(255),
  etl_date DATE NOT NULL,
  CONSTRAINT fk_rp_person_rpid FOREIGN KEY (research_project_id)
    REFERENCES research_project(research_project_id)
);

CREATE TABLE research_project_funding (
  research_project_funding_id NUMERIC(19) NOT NULL PRIMARY KEY,
  research_project_id NUMERIC(19) NOT NULL,
  funding_id VARCHAR2(255) NOT NULL,
  etl_date DATE NOT NULL,
  CONSTRAINT fk_rp_funding_rpid FOREIGN KEY (research_project_id)
    REFERENCES research_project(research_project_id)
);
CREATE INDEX research_project_fund_idx1 ON research_project_funding(research_project_id);

CREATE TABLE research_project_cohort (
  research_project_cohort_id NUMERIC(19) NOT NULL PRIMARY KEY,
  research_project_id NUMERIC(19) NOT NULL,
  etl_date DATE NOT NULL,
  CONSTRAINT fk_rp_cohort_rpid FOREIGN KEY (research_project_id)
    REFERENCES research_project(research_project_id)
);
CREATE INDEX research_project_cohort_idx1 ON research_project_cohort(research_project_id);

CREATE TABLE research_project_irb (
  research_project_irb_id NUMERIC(19) NOT NULL PRIMARY KEY,
  research_project_id NUMERIC(19) NOT NULL,
  research_project_irb VARCHAR2(255) NOT NULL,
  research_project_irb_type VARCHAR2(255) NOT NULL,
  etl_date DATE NOT NULL,
  CONSTRAINT fk_rp_irb_rpid FOREIGN KEY (research_project_id)
    REFERENCES research_project(research_project_id)
);
CREATE INDEX research_project_irb_idx1 ON research_project_irb(research_project_id);

CREATE TABLE product_order (
  product_order_id NUMERIC(19) PRIMARY KEY NOT NULL,
  research_project_id NUMERIC(19) NOT NULL,
  product_id NUMERIC(19) NOT NULL,
  status VARCHAR2(19) NOT NULL,
  created_date DATE NOT NULL,
  modified_date DATE,
  title VARCHAR2(255),
  quote_id VARCHAR2(255),
  jira_ticket_key VARCHAR2(255),
  etl_date DATE NOT NULL,
  CONSTRAINT fk_po_rpid FOREIGN KEY (research_project_id)
    REFERENCES research_project(research_project_id),
  CONSTRAINT fk_po_productid FOREIGN KEY (product_id)
    REFERENCES product(product_id)
);

CREATE INDEX product_order_idx1 ON product_order(research_project_id);
CREATE INDEX product_order_idx2 ON product_order(product_id);

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
  product_order_id NUMERIC(19) NOT NULL,
  sample_name VARCHAR2(255),
  billing_status VARCHAR2(19) NOT NULL,
  etl_date DATE NOT NULL,
  CONSTRAINT fk_pos_poid FOREIGN KEY (product_order_id)
    REFERENCES product_order(product_order_id)
);

CREATE INDEX product_order_sample_idx1 ON product_order_sample(product_order_id);

CREATE TABLE product_order_sample_status (
  product_order_sample_id NUMERIC(19) NOT NULL,
  product_order_id NUMERIC(19),
  status_date DATE NOT NULL,
  billing_status VARCHAR2(19) NOT NULL,
  etl_date DATE NOT NULL,
  CONSTRAINT fk_po_sample_b_s_po_sid FOREIGN KEY (product_order_sample_id)
    REFERENCES product_order_sample(product_order_sample_id),
  PRIMARY KEY (product_order_sample_id, status_date)
);

CREATE TABLE product_order_add_on (
  product_order_add_on_id NUMERIC(19) NOT NULL PRIMARY KEY,
  product_order_id NUMERIC(19) NOT NULL,
  product_id NUMERIC(19) NOT NULL,
  etl_date DATE NOT NULL,
  CONSTRAINT fk_po_add_on_prodid FOREIGN KEY (product_id)
    REFERENCES product(product_id),
  CONSTRAINT fk_po_add_on_poid FOREIGN KEY (product_order_id)
    REFERENCES product_order(product_order_id)
);

CREATE TABLE billable_item (
  billable_item_id NUMERIC(19) NOT NULL PRIMARY KEY,
  product_order_sample_id NUMERIC(19) NOT NULL,
  price_item_id NUMERIC(19) NOT NULL,
  item_count NUMERIC(19) NOT NULL,
  etl_date DATE NOT NULL,
  CONSTRAINT fk_po_billable_item_po_sid FOREIGN KEY (product_order_sample_id)
    REFERENCES product_order_sample(product_order_sample_id),
  CONSTRAINT fk_po_billable_item_piid FOREIGN KEY (price_item_id)
    REFERENCES  price_item(price_item_id)
);

CREATE INDEX billable_item_idx1 ON billable_item (product_order_sample_id, price_item_id);


-- Import tables
DROP TABLE im_billable_item;
DROP TABLE im_product_order_add_on;
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
  current_status VARCHAR2(19),
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
  status_date DATE NOT NULL,
  status VARCHAR2(19)
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
  status VARCHAR2(19),
  created_date DATE,
  modified_date DATE,
  title VARCHAR2(255),
  quote_id VARCHAR2(255),
  jira_ticket_key VARCHAR2(255)
);

CREATE TABLE im_product_order_status (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  product_order_id NUMERIC(19) NOT NULL,
  status_date DATE,
  status VARCHAR2(19)
);

CREATE TABLE im_product_order_sample_stat (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  product_order_sample_id NUMERIC(19) NOT NULL,
  product_order_id NUMERIC(19),
  status_date DATE,
  billing_status VARCHAR2(19)
);


CREATE TABLE im_product_order_sample (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  product_order_sample_id NUMERIC(19) NOT NULL,
  product_order_id NUMERIC(19),
  sample_name VARCHAR2(255),
  billing_status VARCHAR2(19)
);

CREATE TABLE im_product_order_add_on (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  product_order_add_on_id NUMERIC(19) NOT NULL,
  product_order_id NUMERIC(19),
  product_id NUMERIC(19)
);

CREATE TABLE im_billable_item (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  billable_item_id NUMERIC(19) NOT NULL,
  product_order_sample_id NUMERIC(19),
  price_item_id NUMERIC(19),
  item_count NUMERIC(19)
);

COMMIT;
