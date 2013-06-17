-------------------------------------------------------
-- For release 1.26
-------------------------------------------------------
DROP INDEX event_fact_idx2;
CREATE INDEX event_fact_idx2 ON event_fact (product_order_id, sample_name);
