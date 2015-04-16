-------------------------------------------------------
-- For release 1.49
-------------------------------------------------------
-- alter table ledger_entry add quote_server_work_item varchar2(255);
-- alter table im_ledger_entry add quote_server_work_item varchar2(255);

-------------------------------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-3425
-- Create Real time Compliance Report
-------------------------------------------------------
ALTER TABLE PRODUCT_ORDER ADD SKIP_REGULATORY_REASON VARCHAR2(255);
ALTER TABLE IM_PRODUCT_ORDER ADD SKIP_REGULATORY_REASON VARCHAR2(255);

CREATE TABLE pdo_regulatory_infos (
  product_order_id   NUMERIC(19)   NOT NULL,
  regulatory_info_id NUMERIC(19)   NOT NULL,
  identifier         VARCHAR2(255) NOT NULL,
  type               VARCHAR2(255) NOT NULL,
  name               VARCHAR2(255) NOT NULL,
  etl_date           DATE          NOT NULL,
  constraint pk_pdo_regulatory_infos PRIMARY KEY ( product_order_id, regulatory_info_id )
);

alter table pdo_regulatory_infos
add constraint FK_PDO_REGINFO
foreign key(product_order_id)
references product_order(product_order_id) ON DELETE CASCADE;

CREATE INDEX pdo_regulatory_info_idx1 ON pdo_regulatory_infos (regulatory_info_id);


CREATE TABLE im_pdo_regulatory_infos (
  line_number             NUMERIC(9)  NOT NULL,
  etl_date           DATE          NOT NULL,
  is_delete               CHAR(1)     NOT NULL,
  product_order_id   NUMERIC(19),
  regulatory_info_id NUMERIC(19),
  identifier         VARCHAR2(255),
  type               VARCHAR2(255),
  name               VARCHAR2(255)
);

CREATE TABLE im_regulatory_info (
  line_number        NUMERIC(9)    NOT NULL,
  etl_date           DATE          NOT NULL,
  is_delete          CHAR(1)       NOT NULL,
  regulatory_info_id NUMERIC(19),
  identifier         VARCHAR2(255),
  type               VARCHAR2(255),
  name               VARCHAR2(255)
);


