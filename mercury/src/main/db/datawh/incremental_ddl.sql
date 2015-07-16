-------------------------------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-3641
-- Hotfix deploy to MercuryDW, no Mercury code changes required
-------------------------------------------------------
BEGIN
  FOR CUR_REC IN (
    SELECT LAB_VESSEL_ID, QUANT_TYPE, MAX(RUN_DATE) AS NEWEST_RUN_DATE
      FROM LAB_METRIC
    GROUP BY LAB_VESSEL_ID, QUANT_TYPE
    HAVING COUNT(*) > 1 )
  LOOP
    DELETE FROM LAB_METRIC
     WHERE LAB_VESSEL_ID = CUR_REC.LAB_VESSEL_ID
       AND QUANT_TYPE    = CUR_REC.QUANT_TYPE
       AND RUN_DATE      < CUR_REC.NEWEST_RUN_DATE;
  END LOOP;
END;
/

COMMIT;
