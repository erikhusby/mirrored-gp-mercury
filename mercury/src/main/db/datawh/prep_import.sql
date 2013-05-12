/*
 Truncates import tables.
*/
CREATE OR REPLACE PROCEDURE prep_import
IS
BEGIN

execute immediate 'TRUNCATE TABLE im_product_order';
execute immediate 'TRUNCATE TABLE im_product_order_sample';
execute immediate 'TRUNCATE TABLE im_product_order_status';
execute immediate 'TRUNCATE TABLE im_product_order_sample_stat';
execute immediate 'TRUNCATE TABLE im_price_item';
execute immediate 'TRUNCATE TABLE im_product_order_add_on';
execute immediate 'TRUNCATE TABLE im_product';
execute immediate 'TRUNCATE TABLE im_research_project_cohort';
execute immediate 'TRUNCATE TABLE im_research_project';
execute immediate 'TRUNCATE TABLE im_research_project_funding';
execute immediate 'TRUNCATE TABLE im_research_project_irb';
execute immediate 'TRUNCATE TABLE im_research_project_person';
execute immediate 'TRUNCATE TABLE im_research_project_status';
execute immediate 'TRUNCATE TABLE im_lab_batch';
execute immediate 'TRUNCATE TABLE im_lab_vessel';
execute immediate 'TRUNCATE TABLE im_workflow';
execute immediate 'TRUNCATE TABLE im_workflow_process';
execute immediate 'TRUNCATE TABLE im_event_fact';
execute immediate 'TRUNCATE TABLE im_product_order_sample_risk';
execute immediate 'TRUNCATE TABLE im_product_order_sample_bill';
execute immediate 'TRUNCATE TABLE im_ledger_entry';

COMMIT;
END prep_import;

