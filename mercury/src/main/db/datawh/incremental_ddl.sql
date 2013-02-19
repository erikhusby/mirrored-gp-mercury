
-------------------------------------------------------
-- For release 1.17
-------------------------------------------------------
TRUNCATE TABLE event_fact;
TRUNCATE TABLE im_event_fact;
ALTER TABLE event_fact DROP CONSTRAINT fk_event_lab_vessel;
ALTER TABLE event_fact DROP CONSTRAINT fk_event_workflow_config;

ALTER TABLE event_fact ADD (process_id numeric(19) not null);
ALTER TABLE event_fact ADD (sample_name VARCHAR(40));
ALTER TABLE event_fact ADD (workflow_id numeric(19) not null);
ALTER TABLE event_fact DROP COLUMN event_name;
ALTER TABLE event_fact DROP COLUMN sample_key;
ALTER TABLE event_fact DROP COLUMN workflow_config_id;
ALTER TABLE lab_batch DROP COLUMN due_date;
ALTER TABLE lab_batch DROP COLUMN is_active;
ALTER TABLE lab_batch DROP COLUMN created_on;

ALTER TABLE im_event_fact ADD (process_id numeric(19) not null);
ALTER TABLE im_event_fact ADD (sample_name VARCHAR(40));
ALTER TABLE im_event_fact ADD (workflow_id numeric(19) not null);
ALTER TABLE im_event_fact DROP COLUMN event_name;
ALTER TABLE im_event_fact DROP COLUMN sample_key;
ALTER TABLE im_event_fact DROP COLUMN workflow_config_id;
ALTER TABLE im_lab_batch DROP COLUMN due_date;
ALTER TABLE im_lab_batch DROP COLUMN is_active;
ALTER TABLE im_lab_batch DROP COLUMN created_on;

DROP TABLE workflow_config;
DROP TABLE im_workflow_config;

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

ALTER TABLE event_fact ADD CONSTRAINT fk_event_workflow FOREIGN KEY (workflow_id)
  REFERENCES workflow(workflow_id) ON DELETE CASCADE;

ALTER TABLE event_fact ADD CONSTRAINT fk_event_process FOREIGN KEY (process_id)
  REFERENCES workflow_process(process_id) ON DELETE CASCADE;
