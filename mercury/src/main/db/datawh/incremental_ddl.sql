-- --------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-5155
-- Constrain MERCURYDW.LEDGER_ENTRY.VALID_WORK_ITEM to be NULL or a number
-- --------------------------------

ALTER TABLE MERCURYDW.LEDGER_ENTRY
ADD CONSTRAINT VALID_WORK_ITEM CHECK (TO_NUMBER( NVL( QUOTE_SERVER_WORK_ITEM, '1') ) > 0) ENABLE;
