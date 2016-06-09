/* ******* GPLIM-4136
 * Make Designation from Mercury available for reporting
 */

BEGIN
  execute immediate 'drop table flowcell_designation';
  exception when others then null;
END;
/

CREATE TABLE flowcell_designation (
  designation_id      NUMERIC(19) NOT NULL,
  fct_id              NUMERIC(19),
  fct_name            VARCHAR2(255),
  fct_type            VARCHAR2(8),
  designation_library VARCHAR2(255),
  creation_date       DATE,
  flowcell_barcode    VARCHAR2(255),
  flowcell_type       VARCHAR2(255),
  lane                VARCHAR2(32),
  concentration       NUMERIC(19,2),
  is_pool_test        CHAR(1),
  etl_date            DATE        NOT NULL
);

create unique index PK_flowcell_designation
on flowcell_designation ( designation_id );

alter table flowcell_designation
add constraint pk_flowcell_designation primary key ( designation_id) using index PK_flowcell_designation;

-- Other indexes TBD
BEGIN
  execute immediate 'drop table im_fct_create';
  exception when others then null;
END;
/
CREATE TABLE im_fct_create (
  line_number          NUMERIC(9)  NOT NULL,
  etl_date             DATE        NOT NULL,
  is_delete            CHAR(1)     NOT NULL,
  designation_id       NUMERIC(19) NOT NULL,
  fct_id               NUMERIC(19),
  fct_name             VARCHAR2(255),
  fct_type             VARCHAR2(8),
  designation_library  VARCHAR2(255),
  creation_date        DATE,
  flowcell_type        VARCHAR2(255),
  lane                 VARCHAR2(32),
  concentration        NUMERIC(19,2),
  is_pool_test         CHAR(1)
);

BEGIN
  execute immediate 'drop table im_fct_load';
  exception when others then null;
END;
/

CREATE TABLE im_fct_load (
  line_number          NUMERIC(9)  NOT NULL,
  etl_date             DATE        NOT NULL,
  is_delete            CHAR(1)     NOT NULL,
  designation_id       NUMERIC(19) NOT NULL,
  flowcell_barcode     VARCHAR2(255)
);
