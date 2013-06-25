-------------------------------------------------------
-- For release 1.26
-------------------------------------------------------
DROP INDEX event_fact_idx2;
CREATE INDEX event_fact_idx2 ON event_fact (product_order_id, sample_name);

ALTER TABLE im_sequencing_run ADD setup_read_structure  VARCHAR2(255);
ALTER TABLE im_sequencing_run ADD actual_read_structure VARCHAR2(255);

ALTER TABLE sequencing_run ADD setup_read_structure  VARCHAR2(255);
ALTER TABLE sequencing_run ADD actual_read_structure VARCHAR2(255);

ALTER TABLE sequencing_sample_fact add loaded_library_barcode      VARCHAR2(255);
ALTER TABLE sequencing_sample_fact add  loaded_library_create_date  DATE;

ALTER TABLE im_sequencing_sample_fact add loaded_library_barcode      VARCHAR2(255);
ALTER TABLE im_sequencing_sample_fact add  loaded_library_create_date  DATE;

