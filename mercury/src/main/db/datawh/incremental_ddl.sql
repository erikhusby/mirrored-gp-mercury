-------------------------------------------------------
-- For release 1.27
-------------------------------------------------------

ALTER TABLE im_ledger_entry add  price_item_id           NUMERIC(19);
ALTER TABLE im_ledger_entry add  price_item_type         VARCHAR2(50);
ALTER TABLE im_ledger_entry add  quantity                NUMBER(15,4);
ALTER TABLE im_ledger_entry add  billing_session_id      NUMERIC(19);
ALTER TABLE im_ledger_entry add  billing_message         VARCHAR2(500);
ALTER TABLE im_ledger_entry add  work_complete_date      DATE;
