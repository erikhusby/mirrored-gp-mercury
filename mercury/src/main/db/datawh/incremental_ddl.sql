-- GPLIM-6212 requires indexes on array_process_flow table
DROP INDEX IDX_ARRAY_PROCESS_FLOW_PDO_ETL;

CREATE INDEX IDX_ARRAY_PROCESS_FLOW_ETL
ON ARRAY_PROCESS_FLOW( BATCH_NAME, LCSET_SAMPLE_NAME );

-- GPLIM-4108 add column for sap delivery document
alter table ledger_entry add sap_delivery_document varchar2(255);
alter table im_ledger_entry add sap_delivery_document varchar2(255);
