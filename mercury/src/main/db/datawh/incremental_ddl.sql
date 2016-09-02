-- These indexes are on single column which is already the first column in PK, they aren't required
DROP INDEX RESEARCH_PROJECT_STATUS_IDX1;
DROP INDEX PRODUCT_ORDER_STATUS_IDX1;
DROP INDEX PDO_SAMPLE_STATUS_IDX1;

ALTER TABLE PRODUCT_ORDER_SAMPLE ADD (
  BILLING_ETL_DATE DATE DEFAULT SYSDATE
  , RISK_ETL_DATE DATE DEFAULT SYSDATE
  );

UPDATE PRODUCT_ORDER_SAMPLE
SET BILLING_ETL_DATE = ETL_DATE
  , RISK_ETL_DATE = ETL_DATE;

COMMIT;

ALTER TABLE FLOWCELL_DESIGNATION
  ADD FCLOAD_ETL_DATE DATE DEFAULT SYSDATE;

UPDATE FLOWCELL_DESIGNATION
SET FCLOAD_ETL_DATE = ETL_DATE;

COMMIT;

-- Clean up old status data that had a new row for every data change whether or not the status changed
-- Only keep first status and first status change in a string of duplicates by date
DELETE FROM PRODUCT_ORDER_STATUS
WHERE ROWID
      IN ( SELECT BASE_ROW
           FROM ( SELECT ROWID AS BASE_ROW
                    , STATUS
                    , LAG ( STATUS ) OVER
             ( PARTITION BY PRODUCT_ORDER_ID ORDER BY STATUS_DATE, STATUS ) AS PREV_STATUS
                  FROM PRODUCT_ORDER_STATUS )
           WHERE PREV_STATUS IS NOT NULL AND STATUS = PREV_STATUS  );
COMMIT;

DELETE FROM RESEARCH_PROJECT_STATUS
WHERE ROWID
      IN ( SELECT BASE_ROW
           FROM ( SELECT ROWID AS BASE_ROW
                    , STATUS
                    , LAG ( STATUS ) OVER
             ( PARTITION BY RESEARCH_PROJECT_ID
               ORDER BY STATUS_DATE, STATUS
             ) AS PREV_STATUS
                  FROM RESEARCH_PROJECT_STATUS )
           WHERE PREV_STATUS IS NOT NULL AND STATUS = PREV_STATUS  );
COMMIT;

DELETE FROM PRODUCT_ORDER_SAMPLE_STATUS
WHERE ROWID
      IN ( SELECT BASE_ROW
           FROM ( SELECT ROWID AS BASE_ROW
                    , DELIVERY_STATUS
                    , LAG ( DELIVERY_STATUS ) OVER
             ( PARTITION BY PRODUCT_ORDER_SAMPLE_ID ORDER BY STATUS_DATE, DELIVERY_STATUS ) AS PREV_STATUS
                  FROM PRODUCT_ORDER_SAMPLE_STATUS )
           WHERE PREV_STATUS IS NOT NULL AND DELIVERY_STATUS = PREV_STATUS  );
COMMIT;

ALTER TABLE im_event_fact
ADD (
  library_name varchar2(64)
);

ALTER TABLE event_fact
ADD (
  library_name varchar2(64)
);

ALTER TABLE IM_LAB_VESSEL ADD NAME VARCHAR2(255);
ALTER TABLE LAB_VESSEL ADD NAME VARCHAR2(255);
