-- GPLIM-6069 Add event operator
ALTER TABLE MERCURYDW.IM_EVENT_FACT
    ADD OPERATOR VARCHAR2(32);
ALTER TABLE MERCURYDW.EVENT_FACT
    ADD OPERATOR VARCHAR2(32);

CREATE TABLE MERCURYDW.IM_EVENT_METADATA
(
    LINE_NUMBER   NUMBER(9, 0),
    ETL_DATE      DATE,
    IS_DELETE     CHAR,
    METADATA_ID   NUMBER(19, 0),
    LAB_EVENT_ID  NUMBER(19, 0),
    METADATA_TYPE VARCHAR2(32),
    VALUE         VARCHAR2(255)
);

CREATE TABLE MERCURYDW.EVENT_METADATA
(
    METADATA_ID   NUMBER(19, 0) NOT NULL,
    LAB_EVENT_ID  NUMBER(19, 0) NOT NULL,
    METADATA_TYPE VARCHAR2(32)  NOT NULL,
    VALUE         VARCHAR2(255),
    ETL_DATE      DATE          NOT NULL
);

CREATE INDEX IDX_EVENT_METADATA
    ON MERCURYDW.EVENT_METADATA (METADATA_ID);
CREATE INDEX IDX_EVENT_METADATA_EVENT
    ON MERCURYDW.EVENT_METADATA (LAB_EVENT_ID);

-- Generate event_fact.operator backfill file event_operator.dat
-- by executing org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventFixupTest.extractEventOperatorList
-- Full path of file shown in System.out output

-- Create table to load eventId - operator data exported from Mercury
CREATE TABLE MERCURYDW.TMP_EVENT_OPERATOR_MAP
(
    EVENT_ID NUMBER(19, 0),
    OPERATOR VARCHAR2(32)
);

-- (Optional - use whatever tool is handy to load file event_operator.dat into table MERCURYDW.TMP_EVENT_OPERATOR_MAP)
-- Using Oracle client tool sqlldr:
-- Navigate to folder or copy output file event_operator.dat
--   of
-- Save this text in file event_operator.ctl in same folder:
OPTIONS
    (ERRORS=50,DIRECT= TRUE)
    LOAD DATA
INFILE 'event_operator.dat'
REPLACE
INTO TABLE MERCURYDW.TMP_EVENT_OPERATOR_MAP
FIELDS TERMINATED BY ','
( EVENT_ID,
  OPERATOR CHAR (32) )

-- Change password and IP address and execute sqlldr to load data:
sqlldr userid="mercurydw/****@seqdev.broad.mit.edu:1521/seqdev3" control=event_operator.ctl log=event_operator.log bad=event_operator.bad discard=event_operator.dsc

-- Script to bulk update event_fact with operator names:  (15-20 minutes - run after hours)
DECLARE
    TYPE ID_ARR_TY IS TABLE OF NUMBER(19) INDEX BY BINARY_INTEGER;
    TYPE USER_ARR_TY IS TABLE OF VARCHAR2(32) INDEX BY BINARY_INTEGER;
    V_ID_ARR   ID_ARR_TY;
    V_USER_ARR USER_ARR_TY;
    CURSOR CUR_SRC IS SELECT EVENT_ID, OPERATOR
                      FROM MERCURYDW.TMP_EVENT_OPERATOR_MAP;
BEGIN
    OPEN CUR_SRC;
    WHILE (TRUE)
        LOOP
            FETCH CUR_SRC BULK COLLECT INTO V_ID_ARR, V_USER_ARR LIMIT 100000;
            EXIT WHEN V_ID_ARR.COUNT = 0;
            FORALL IDX IN V_ID_ARR.FIRST .. V_ID_ARR.LAST
                UPDATE EVENT_FACT
                SET OPERATOR = V_USER_ARR(IDX)
                WHERE LAB_EVENT_ID = V_ID_ARR(IDX);
            COMMIT; -- Each batch
        END LOOP;
    CLOSE CUR_SRC;
END;
/

-- Drop when backfill script is complete
DROP TABLE MERCURYDW.TMP_EVENT_OPERATOR_MAP;