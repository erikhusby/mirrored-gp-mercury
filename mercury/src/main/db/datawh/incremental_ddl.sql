
-------------------------------------------------------
-- For release 1.23
-------------------------------------------------------
alter table product_order add (placed_date DATE);
alter table im_product_order add (placed_date DATE);
alter table product_order_sample add (ledger_quote_id VARCHAR2(255));
CREATE TABLE im_ledger_entry (
  line_number NUMERIC(9) NOT NULL,
  etl_date DATE NOT NULL,
  is_delete CHAR(1) NOT NULL,
  ledger_id NUMERIC(19) NOT NULL,
  product_order_sample_id NUMERIC(19),
  quote_id VARCHAR2(255)
);
alter table im_research_project add (parent_research_project_id numeric(19,0));
alter table im_research_project add (root_research_project_id numeric(19,0));
alter table research_project add (parent_research_project_id numeric(19,0));
alter table research_project add (root_research_project_id numeric(19,0));

create index ix_parent_project on research_project (parent_research_project_id);
create index ix_root_project on research_project (root_research_project_id);
