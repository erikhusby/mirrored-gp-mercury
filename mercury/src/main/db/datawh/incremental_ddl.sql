-- --------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-5070
-- Support array pipeline blacklist and abandoned vessel
-- --------------------------------
DROP TABLE MERCURYDW.IM_ABANDON_VESSEL_POSITION;

DROP TABLE MERCURYDW.IM_ABANDON_VESSEL;

CREATE TABLE MERCURYDW.IM_ABANDON_VESSEL (
  LINE_NUMBER NUMBER(9,0),
  ETL_DATE DATE,
  IS_DELETE CHAR,
  ABANDON_ID NUMBER(19,0),
  ABANDON_VESSEL_ID NUMBER(19,0),
  REASON VARCHAR2(64),
  ABANDONED_ON DATE,
  VESSEL_POSITION VARCHAR2(32)
);


