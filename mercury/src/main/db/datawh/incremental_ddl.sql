-------------------------------------------------------
-- For release 1.49
-------------------------------------------------------
-- alter table ledger_entry add quote_server_work_item varchar2(255);
-- alter table im_ledger_entry add quote_server_work_item varchar2(255);

-------------------------------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-3425
-- Create Real time Compliance Report
-------------------------------------------------------
ALTER TABLE PRODUCT_ORDER    ADD SKIP_REGULATORY_REASON VARCHAR2(255);
ALTER TABLE IM_PRODUCT_ORDER ADD (
  SKIP_REGULATORY_REASON VARCHAR2(255),
  REG_INFO_IDS VARCHAR2(255) );

--DROP TABLE pdo_regulatory_infos;

CREATE TABLE pdo_regulatory_infos (
  product_order    NUMERIC(19)   NOT NULL,
  regulatory_infos NUMERIC(19)   NOT NULL,
  etl_date         DATE          NOT NULL,
  constraint pk_pdo_regulatory_infos PRIMARY KEY ( product_order, regulatory_infos )
);

--DROP TABLE regulatory_info;

CREATE TABLE regulatory_info (
  regulatory_info_id NUMERIC(19) NOT NULL PRIMARY KEY,
  identifier         VARCHAR2(255),
  type               VARCHAR2(255),
  name               VARCHAR2(255),
  etl_date           DATE          NOT NULL
);

alter table pdo_regulatory_infos
add constraint FK_PDO_REGINFO
foreign key(product_order)
references product_order(product_order_id) ON DELETE CASCADE;

alter table pdo_regulatory_infos
add constraint FK_REGINFO_PDO
foreign key(regulatory_infos)
references regulatory_info(regulatory_info_id) ON DELETE CASCADE;

CREATE INDEX pdo_regulatory_infos_idx1 ON pdo_regulatory_infos (regulatory_infos);

--DROP TABLE im_regulatory_info;

CREATE TABLE im_regulatory_info (
  line_number        NUMERIC(9)    NOT NULL,
  etl_date           DATE          NOT NULL,
  is_delete          CHAR(1)       NOT NULL,
  regulatory_info_id NUMERIC(19),
  identifier         VARCHAR2(255),
  type               VARCHAR2(255),
  name               VARCHAR2(255)
);


