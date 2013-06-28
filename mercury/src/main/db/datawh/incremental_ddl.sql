-------------------------------------------------------
-- For release 1.27
-------------------------------------------------------

CREATE TABLE ledger_entry (
  ledger_id               NUMERIC(19) NOT NULL PRIMARY KEY,
  product_order_sample_id NUMERIC(19),
  quote_id                VARCHAR2(255),
  price_item_id           NUMERIC(19),
  price_item_type         VARCHAR2(50),
  quantity                NUMBER(15,4),
  billing_session_id      NUMERIC(19),
  billing_message         VARCHAR2(500),
  work_complete_date      DATE,
  etl_date                DATE        NOT NULL
);

ALTER TABLE im_ledger_entry add  price_item_id           NUMERIC(19);
ALTER TABLE im_ledger_entry add  price_item_type         VARCHAR2(50);
ALTER TABLE im_ledger_entry add  quantity                NUMBER(15,4);
ALTER TABLE im_ledger_entry add  billing_session_id      NUMERIC(19);
ALTER TABLE im_ledger_entry add  billing_message         VARCHAR2(500);
ALTER TABLE im_ledger_entry add  work_complete_date      DATE;


CREATE TABLE billing_session (
  billing_session_id      NUMERIC(19) NOT NULL PRIMARY KEY,
  billed_date             DATE,
  billing_session_type    VARCHAR2(50),
  etl_date                DATE        NOT NULL
);

CREATE TABLE im_billing_session (
  line_number             NUMERIC(9)  NOT NULL,
  etl_date                DATE        NOT NULL,
  is_delete               CHAR(1)     NOT NULL,
  billing_session_id      NUMERIC(19) NOT NULL,
  billed_date             DATE,
  billing_session_type    VARCHAR2(50)
);
