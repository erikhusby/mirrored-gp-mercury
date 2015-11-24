-------------------------------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-3557
-- Create Ancestry ETL

DROP TABLE IM_EVENT_FACT;

CREATE TABLE MERCURYDW.IM_EVENT_FACT (
  LINE_NUMBER NUMBER(9) NOT NULL,
  ETL_DATE DATE NOT NULL,
  IS_DELETE CHAR NOT NULL,
  LAB_EVENT_ID NUMBER(19),
  WORKFLOW_ID NUMBER(19),
  PROCESS_ID NUMBER(19),
  LAB_EVENT_TYPE VARCHAR2(64),
  PRODUCT_ORDER_ID NUMBER(19),
  SAMPLE_NAME VARCHAR2(40),
  LCSET_SAMPLE_NAME VARCHAR2(40),
  BATCH_NAME VARCHAR2(40),
  STATION_NAME VARCHAR2(255),
  LAB_VESSEL_ID NUMBER(19),
  POSITION VARCHAR2(32),
  PROGRAM_NAME VARCHAR2(255),
  MOLECULAR_INDEXING_SCHEME VARCHAR2(255),
  EVENT_DATE DATE,
  EVENT_FACT_ID NUMBER(28) );

ALTER TABLE MERCURYDW.EVENT_FACT RENAME TO EVENT_FACT_BAK;
ALTER TABLE MERCURYDW.EVENT_FACT_BAK DROP CONSTRAINT FK_EVENT_LAB_VESSEL;
ALTER TABLE MERCURYDW.EVENT_FACT_BAK DROP CONSTRAINT FK_EVENT_PDO;
ALTER TABLE MERCURYDW.EVENT_FACT_BAK DROP CONSTRAINT FK_EVENT_WORKFLOW;
ALTER TABLE MERCURYDW.EVENT_FACT_BAK DROP CONSTRAINT FK_EVENT_PROCESS;
DROP INDEX MERCURYDW.EVENT_FACT_IDX1;
DROP INDEX MERCURYDW.EVENT_FACT_IDX2;
DROP INDEX MERCURYDW.EVENT_FACT_IDX3;
-- Ignore not exists error
DROP INDEX MERCURYDW.IDX_EVENT_VESSEL;

CREATE TABLE MERCURYDW.EVENT_FACT(
  EVENT_FACT_ID NUMBER(28) NOT NULL,
  LAB_EVENT_ID NUMBER(19) NOT NULL,
  WORKFLOW_ID NUMBER(19),
  PROCESS_ID NUMBER(19),
  LAB_EVENT_TYPE VARCHAR2(64),
  PRODUCT_ORDER_ID NUMBER(19),
  SAMPLE_NAME VARCHAR2(40),
  LCSET_SAMPLE_NAME VARCHAR2(40),
  BATCH_NAME VARCHAR2(40),
  STATION_NAME VARCHAR2(255),
  LAB_VESSEL_ID NUMBER(19),
  POSITION VARCHAR2(32),
  EVENT_DATE DATE NOT NULL,
  PROGRAM_NAME VARCHAR2(255),
  MOLECULAR_INDEXING_SCHEME VARCHAR2(255),
  ETL_DATE DATE NOT NULL,
  PRIMARY KEY (EVENT_FACT_ID),
  CONSTRAINT FK_EVENT_LAB_VESSEL FOREIGN KEY (LAB_VESSEL_ID)
  REFERENCES MERCURYDW.LAB_VESSEL (LAB_VESSEL_ID) ON DELETE CASCADE ENABLE,
  CONSTRAINT FK_EVENT_PDO FOREIGN KEY (PRODUCT_ORDER_ID)
  REFERENCES MERCURYDW.PRODUCT_ORDER (PRODUCT_ORDER_ID) ON DELETE CASCADE ENABLE,
  CONSTRAINT FK_EVENT_WORKFLOW FOREIGN KEY (WORKFLOW_ID)
  REFERENCES MERCURYDW.WORKFLOW (WORKFLOW_ID) ON DELETE CASCADE ENABLE,
  CONSTRAINT FK_EVENT_PROCESS FOREIGN KEY (PROCESS_ID)
  REFERENCES MERCURYDW.WORKFLOW_PROCESS (PROCESS_ID) ON DELETE CASCADE ENABLE );

DROP SEQUENCE event_fact_id_seq;
CREATE SEQUENCE event_fact_id_seq START WITH 1;

CREATE INDEX MERCURYDW.EVENT_FACT_IDX1 ON MERCURYDW.EVENT_FACT (EVENT_DATE);
CREATE INDEX MERCURYDW.EVENT_FACT_IDX2 ON MERCURYDW.EVENT_FACT (PRODUCT_ORDER_ID, SAMPLE_NAME);
CREATE INDEX MERCURYDW.EVENT_FACT_IDX3 ON MERCURYDW.EVENT_FACT (LAB_EVENT_ID);
CREATE INDEX MERCURYDW.IDX_EVENT_VESSEL ON MERCURYDW.EVENT_FACT (LAB_VESSEL_ID);


CREATE TABLE im_library_ancestry
(
  line_number               NUMERIC(9) NOT NULL,
  etl_date                  DATE NOT NULL,
  is_delete                 CHAR(1) NOT NULL,
  ancestor_event_id         NUMBER(19) NOT NULL,
  ancestor_library_id       NUMBER(19) NOT NULL,
  ancestor_library_type     VARCHAR2(255) NOT NULL,
  ancestor_library_creation DATE NOT NULL,
  child_event_id            NUMBER(19) NOT NULL,
  child_library_id          NUMBER(19) NOT NULL,
  child_library_type        VARCHAR2(255) NOT NULL,
  child_library_creation    DATE NOT NULL
);

CREATE TABLE library_ancestry
(
  ancestor_event_id         NUMBER(19) NOT NULL,
  ancestor_library_id       NUMBER(19) NOT NULL,
  ancestor_library_type     VARCHAR2(255) NOT NULL,
  ancestor_library_creation DATE NOT NULL,
  child_event_id            NUMBER(19) NOT NULL,
  child_library_id          NUMBER(19) NOT NULL,
  child_library_type        VARCHAR2(255) NOT NULL,
  child_library_creation    DATE NOT NULL,
  etl_date                  DATE NOT NULL
);

CREATE UNIQUE INDEX PK_ANCESTRY on library_ancestry (child_library_id, ancestor_library_id, child_event_id, ancestor_event_id );
CREATE INDEX idx_ancestry_reverse on library_ancestry (ancestor_library_id, child_library_id );
CREATE UNIQUE INDEX IDX_VESSEL_LABEL ON LAB_VESSEL( LABEL ) COMPUTE STATISTICS;

-- Warehouse query performance
-- ETL delete performance
CREATE INDEX IDX_ANCESTRY_CHILD_EVENT ON LIBRARY_ANCESTRY( CHILD_EVENT_ID );
DROP PROCEDURE MERGE_IMPORT;



-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-3763
-- Add metadata to PRODUCT_ORDER_SAMPLE
-- Note:  Must be run prior to deploy (all columns nullable - deploy of ticket GPLIM-3763 not required)
-------------------------------------------------------
ALTER TABLE IM_PRODUCT_ORDER_SAMPLE
ADD (
  PARTICIPANT_ID VARCHAR2(255) NULL,
  SAMPLE_TYPE VARCHAR2(255) NULL,
  SAMPLE_RECEIPT DATE NULL,
  ORIGINAL_SAMPLE_TYPE VARCHAR2(255) NULL );

-- Append to end of table
-- todo jms - restructure table to insert new columns in more logical order?
ALTER TABLE PRODUCT_ORDER_SAMPLE
ADD (
  PARTICIPANT_ID VARCHAR2(255) NULL,
  SAMPLE_TYPE VARCHAR2(255) NULL,
  SAMPLE_RECEIPT DATE NULL,
  ORIGINAL_SAMPLE_TYPE VARCHAR2(255) NULL );


-- https://gpinfojira.broadinstitute.org/jira/browse/SUPPORT-1275
-- Concatenate multiple 'Risk Types' in consistent order
-- Update historical data to avoid backfill
-- Note:  Can be run multiple times any time regardless of deploy (must be run once after deploy)
-------------------------------------------------------
-- Before and after test query
--SELECT PRODUCT_ORDER_SAMPLE_ID,
--       RISK_TYPES
--  FROM PRODUCT_ORDER_SAMPLE
-- WHERE RISK_TYPES IS NOT NULL
--   AND RISK_TYPES LIKE '% AND %'
--   AND PRODUCT_ORDER_SAMPLE_ID BETWEEN 60197 AND 60238
--ORDER BY PRODUCT_ORDER_SAMPLE_ID

DECLARE

  TYPE nbr_array_type IS TABLE OF NUMBER INDEX BY BINARY_INTEGER;
  v_id_arr nbr_array_type;

  TYPE txt_array_type IS TABLE OF VARCHAR2(255) INDEX BY BINARY_INTEGER;
  risk_arr txt_array_type;

  CURSOR cur_risk
  IS
    SELECT PRODUCT_ORDER_SAMPLE_ID,
      RISK_TYPES
    FROM PRODUCT_ORDER_SAMPLE
    WHERE RISK_TYPES IS NOT NULL
          AND RISK_TYPES LIKE '% AND %';
  --AND ROWNUM < 10;

  v_sqlerrmsg VARCHAR2(1024);

  FUNCTION sort_types( a_risk VARCHAR2 ) RETURN VARCHAR2
  IS
    v_types_varr SYS.TXNAME_ARRAY;

    v_cur_pos  NUMBER(4);
    v_next_pos NUMBER(4);
    v_count NUMBER(4);
    v_token CONSTANT CHAR(5) := ' AND ';
    v_segment VARCHAR2(255);

    BEGIN
      -- DBMS_OUTPUT.PUT_LINE('RISK: ' || a_risk);

      v_types_varr := SYS.TXNAME_ARRAY();

      -- PL/SQL Split() implementation
      v_cur_pos := 1;
      v_count := 0;
      LOOP
        v_next_pos := INSTR( a_risk, v_token, v_cur_pos );
        -- DBMS_OUTPUT.PUT_LINE('NEXT: ' || v_next_pos);
        IF v_next_pos = 1 THEN
          -- Begins with token border case
          v_cur_pos := LENGTH(v_token) + 1;
          CONTINUE;
        ELSIF v_next_pos = 0 THEN
          v_segment := SUBSTR( a_risk, v_cur_pos, LENGTH(a_risk) - v_cur_pos + 1);
        ELSE
          v_segment := SUBSTR( a_risk, v_cur_pos, v_next_pos - v_cur_pos );
          v_cur_pos := v_next_pos + LENGTH(v_token);
        END IF;
        v_count := v_count + 1;
        v_types_varr.EXTEND(1);
        v_types_varr( v_count ) := v_segment;
        EXIT WHEN v_next_pos = 0;
      END LOOP;

      -- Reuse variable
      v_segment := '';

      -- Let Oracle sort
      FOR cur_rec IN ( SELECT COLUMN_VALUE AS token
                       FROM TABLE (v_types_varr)
                       ORDER BY 1 ) LOOP
        v_segment := v_segment || v_token || cur_rec.token;
      END LOOP;

      -- Strip off leading token and return
      -- DBMS_OUTPUT.PUT_LINE('DIGESTED: ' || SUBSTR( v_segment, 6 ));
      RETURN SUBSTR( v_segment, 6 );

    END;

BEGIN

  -- Blow up
  IF sort_types(' AND A') <> 'A' THEN
    RAISE_APPLICATION_ERROR( -20001, '*** SORT FAIL > [ AND A]');
    NULL;
  END IF;

  IF sort_types('Corn AND Apple AND Bread') <> 'Apple AND Bread AND Corn' THEN
    RAISE_APPLICATION_ERROR( -20001, '*** SORT FAIL > [Corn AND Apple AND Bread]');
  END IF;

  IF sort_types('C AND A AND A') <> 'A AND A AND C' THEN
    RAISE_APPLICATION_ERROR( -20001, '*** SORT FAIL > [A AND C AND B]');
  END IF;

  IF sort_types('A AND C AND B') <> 'A AND B AND C' THEN
    RAISE_APPLICATION_ERROR( -20001, '*** SORT FAIL > [A AND C AND B]');
  END IF;

  IF sort_types('Is FFPE AND Concentration AND Total DNA') <> 'Concentration AND Is FFPE AND Total DNA' THEN
    RAISE_APPLICATION_ERROR( -20001, '*** SORT FAIL > [Is FFPE AND Concentration AND Total DNA]');
  END IF;

  OPEN cur_risk;

  LOOP
    FETCH cur_risk BULK COLLECT INTO v_id_arr, risk_arr LIMIT 5000;
    EXIT WHEN v_id_arr.COUNT = 0;
    FOR idx IN v_id_arr.FIRST .. v_id_arr.LAST
    LOOP
      risk_arr(idx) := sort_types(risk_arr(idx));
    END LOOP;

    FORALL idx2 IN v_id_arr.FIRST .. v_id_arr.LAST
    UPDATE PRODUCT_ORDER_SAMPLE
    SET RISK_TYPES = risk_arr(idx2)
    WHERE PRODUCT_ORDER_SAMPLE_ID = v_id_arr(idx2);

  END LOOP;

  CLOSE cur_risk;

  EXCEPTION
  WHEN OTHERS THEN
  v_sqlerrmsg := TO_CHAR(SQLCODE) || ':' || SUBSTR(SQLERRM, 1, 200);
  ROLLBACK;
  RAISE_APPLICATION_ERROR( -20001, v_sqlerrmsg);
END;
/

-- Test
--ROLLBACK;
COMMIT;
