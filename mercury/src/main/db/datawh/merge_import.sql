/*
 Does insert/update/delete to all reporting tables, using data from import tables.
*/
CREATE OR REPLACE PACKAGE MERGE_ETL_IMPORT
AS
  PROCEDURE DO_ETL;
END MERGE_ETL_IMPORT;
/

SHOW ERRORS

CREATE OR REPLACE PACKAGE BODY MERGE_ETL_IMPORT
AS

  errmsg        VARCHAR2(255);
  v_tmp  NUMBER;

  TYPE PK_ARR_TY IS TABLE OF NUMBER(19) INDEX BY BINARY_INTEGER;
  V_PK_ARR PK_ARR_TY;

  -- Use same modification timestamp for all ETL methods in call to DO_ETL()
  V_ETL_MOD_TIMESTAMP DATE;

  /* *****************************
   * Utility function to mimic typical (e.g. Java) split() functionality
   * Splits String output of java.util.Arrays.toString output into array of NUMBER(19)
   * Elements enclosed in square brackets ("[]")delimited by the characters ", " (a comma followed by a space).
   * e.g. [1]  [1, 2, 3, 4]
   ***************** */
  FUNCTION PKS_FROM_JAVA_ARRAYSTOSTRING( V_ARRAYSTR IN VARCHAR2 )
    RETURN PK_ARR_TY
  IS
    V_CUR_POS  NUMBER(4);
    V_NEXT_POS NUMBER(4);
    V_VAL      VARCHAR2(19);
    V_PK_ARR   PK_ARR_TY;
    V_INPUT    VARCHAR2(1024);
  BEGIN
    V_INPUT := TRIM( V_ARRAYSTR );

    -- Nothing to do, return empty array
    IF V_INPUT IS NULL OR LENGTH(V_INPUT) < 3 THEN
      RETURN V_PK_ARR;
    END IF;

    -- Wish there was a split() function...
    V_CUR_POS := 2;
    LOOP
      V_NEXT_POS := INSTR( V_INPUT, ',', V_CUR_POS );
      IF V_NEXT_POS = 0 THEN
        V_VAL := SUBSTR( V_INPUT, V_CUR_POS, LENGTH(V_INPUT) - V_CUR_POS);
      ELSE
        V_VAL := SUBSTR( V_INPUT, V_CUR_POS, V_NEXT_POS - V_CUR_POS );
        V_CUR_POS := V_NEXT_POS + 1;
      END IF;
      V_PK_ARR( V_PK_ARR.COUNT + 1 ) := TO_NUMBER( TRIM( V_VAL ) );
      EXIT WHEN V_NEXT_POS = 0;
    END LOOP;

    RETURN V_PK_ARR;

  END PKS_FROM_JAVA_ARRAYSTOSTRING;

  PROCEDURE SHOW_ETL_STATS( A_UPD_COUNT PLS_INTEGER, A_INS_COUNT PLS_INTEGER, A_TBL_NAME VARCHAR2 )
  IS
  BEGIN
    DBMS_OUTPUT.PUT_LINE('Table ' || A_TBL_NAME || ': ' || A_UPD_COUNT || ' updates, ' || A_INS_COUNT || ' inserts.' );
  END SHOW_ETL_STATS;

  /* ************************
   * Deletes rows in the reporting tables when the import has is_delete = 'T'.
   * Does the most dependent (FK dependency) tables first.
   ****************** */
  PROCEDURE DO_DELETES
  AS
    V_PK_DEL_ARR PK_ARR_TY;
    BEGIN

      DELETE FROM flowcell_designation
      WHERE designation_id IN (
        SELECT batch_starting_vessel_id
        FROM im_fct_create
        WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' flowcell_designation (lab batch vessel ETL) rows' );

      -- Process (rare - from fix-up tests) raw deletes related to events
      DELETE FROM event_fact
       WHERE lab_event_id IN (
             SELECT d.lab_event_id
               FROM im_event_fact d
              WHERE d.is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' event_fact rows' );

      DELETE FROM LIBRARY_LCSET_SAMPLE_BASE
       WHERE library_event_id IN (
              SELECT d.lab_event_id
                FROM im_event_fact d
               WHERE d.is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' library_lcset_sample_base rows' );

      DELETE FROM LIBRARY_ANCESTRY
      WHERE CHILD_EVENT_ID IN (
            SELECT d.lab_event_id
              FROM im_event_fact d
             WHERE d.is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' library_ancestry child rows' );

      -- Tables event_fact, library_ancestry, and library_lcset_sample_base require full refresh
      --   Clear data ONLY IF newer ETL than already saved.
      SELECT DISTINCT im.lab_event_id
      BULK COLLECT INTO V_PK_DEL_ARR
        FROM event_fact e
         --  Avoid quadratic joins
          , ( SELECT DISTINCT lab_event_id, etl_date
                FROM im_event_fact
               WHERE is_delete = 'F' ) im
       WHERE e.lab_event_id = im.lab_event_id
         AND e.etl_date     <= im.etl_date;

      IF V_PK_DEL_ARR.COUNT > 0 THEN
        -- Wipe links in LIBRARY_LCSET_SAMPLE_BASE
        FORALL IDX IN V_PK_DEL_ARR.FIRST .. V_PK_DEL_ARR.LAST
        DELETE FROM LIBRARY_LCSET_SAMPLE_BASE
         WHERE library_event_id = V_PK_DEL_ARR(IDX);
        DBMS_OUTPUT.PUT_LINE( 'Pre-deleted ' || SQL%ROWCOUNT || ' updates to library_lcset_sample_base rows' );

        -- Wipe child and all ancestors from LIBRARY_ANCESTRY
        FORALL IDX IN V_PK_DEL_ARR.FIRST .. V_PK_DEL_ARR.LAST
        DELETE FROM LIBRARY_ANCESTRY
         WHERE CHILD_EVENT_ID = V_PK_DEL_ARR(IDX);
        DBMS_OUTPUT.PUT_LINE( 'Pre-deleted ' || SQL%ROWCOUNT || ' updates to library_ancestry child rows' );

        -- Finally, delete from event_fact
        FORALL IDX IN V_PK_DEL_ARR.FIRST .. V_PK_DEL_ARR.LAST
        DELETE FROM EVENT_FACT
        WHERE LAB_EVENT_ID = V_PK_DEL_ARR(IDX);
        DBMS_OUTPUT.PUT_LINE( 'Pre-deleted ' || SQL%ROWCOUNT || ' updates to event_fact rows' );
      END IF;


      -- ****************
      -- Delete older event related rows from import tables
      SELECT DISTINCT im.lab_event_id
      BULK COLLECT INTO V_PK_DEL_ARR
      FROM event_fact e
         , ( SELECT DISTINCT lab_event_id, etl_date
               FROM im_event_fact
              WHERE is_delete = 'F' ) im
      WHERE e.lab_event_id = im.lab_event_id
        AND e.etl_date    > im.etl_date;

      IF V_PK_DEL_ARR.COUNT > 0 THEN
        FORALL IDX IN V_PK_DEL_ARR.FIRST .. V_PK_DEL_ARR.LAST
        DELETE FROM im_event_fact
        WHERE lab_event_id = V_PK_DEL_ARR(IDX);

        FORALL IDX IN V_PK_DEL_ARR.FIRST .. V_PK_DEL_ARR.LAST
        DELETE FROM im_library_ancestry
        WHERE CHILD_EVENT_ID = V_PK_DEL_ARR(IDX);
      END IF;
      -- ****************

      DELETE FROM product_order_sample
      WHERE product_order_sample_id IN (
        SELECT product_order_sample_id
          FROM im_product_order_sample
         WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' product_order_sample rows' );

      DELETE FROM research_project_status
      WHERE research_project_id IN (
        SELECT research_project_id
        FROM im_research_project_status
        WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' research_project_status rows' );

      DELETE FROM research_project_person
      WHERE research_project_person_id IN (
        SELECT research_project_person_id
        FROM im_research_project_person
        WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' research_project_person rows' );

      DELETE FROM research_project_funding
      WHERE research_project_funding_id IN (
        SELECT research_project_funding_id
        FROM im_research_project_funding
        WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' research_project_funding rows' );

      DELETE FROM research_project_cohort
      WHERE research_project_cohort_id IN (
        SELECT research_project_cohort_id
        FROM im_research_project_cohort
        WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' research_project_cohort rows' );

      DELETE FROM research_project_irb
      WHERE research_project_irb_id IN (
        SELECT research_project_irb_id
        FROM im_research_project_irb
        WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' research_project_irb rows' );

      DELETE FROM product_order_add_on
      WHERE product_order_add_on_id IN (
        SELECT product_order_add_on_id
        FROM im_product_order_add_on
        WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' product_order_add_on rows' );

      DELETE FROM price_item
      WHERE price_item_id IN (
        SELECT
          price_item_id
        FROM im_price_item
        WHERE is_delete = 'T'
      );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' price_item rows' );

      -- Flush PDO regulatory info for all deleted product orders
      DELETE FROM pdo_regulatory_infos
      WHERE product_order IN (
        SELECT product_order_id
        FROM im_product_order
        WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' pdo_regulatory_infos rows' );

      -- Doubtful regulatory info will ever be deleted, but handle it
      DELETE FROM regulatory_info
      WHERE regulatory_info_id IN (
        SELECT regulatory_info_id
        FROM im_regulatory_info
        WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' regulatory_info rows' );

      DELETE FROM product_order
      WHERE product_order_id IN (
        SELECT product_order_id
        FROM im_product_order
        WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' product_order rows' );

      DELETE FROM lab_vessel
      WHERE lab_vessel_id IN (
        SELECT lab_vessel_id
        FROM im_lab_vessel
        WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' lab_vessel rows' );

      DELETE FROM abandon_vessel
      WHERE abandon_type = 'AbandonVessel'
        AND abandon_id IN (
        SELECT abandon_id
        FROM im_abandon_vessel
        WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' abandon_vessel rows' );

      DELETE FROM lab_metric
      WHERE lab_metric_id IN (
        SELECT lab_metric_id
        FROM im_lab_metric
        WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' lab_metric rows' );

      DELETE FROM product
      WHERE product_id IN (
        SELECT product_id
        FROM im_product
        WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' product rows' );

      DELETE FROM research_project
      WHERE research_project_id IN (
        SELECT research_project_id
        FROM im_research_project
        WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' research_project rows' );

      DELETE FROM sequencing_run
      WHERE sequencing_run_id IN (
        SELECT sequencing_run_id
        FROM im_sequencing_run
        WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' sequencing_run rows' );

      DELETE FROM ledger_entry
      WHERE ledger_id IN (
        SELECT ledger_id
        FROM im_ledger_entry
        WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' ledger_entry rows' );

      DELETE FROM billing_session
      WHERE billing_session_id IN (
        SELECT billing_session_id
        FROM im_billing_session
        WHERE is_delete = 'T' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' billing_session rows' );

      -- For this fact table, a re-export of audited entities should replace
      -- existing ones.  Sequencing will reuse flowcell barcode when redoing a
      -- run, so replace based on flowcell barcode too, provided the imported
      -- sequencing run is later than the exiting one.
      -- **********
      -- Flag rows we want to delete from import
      UPDATE im_sequencing_sample_fact
      SET sequencing_sample_fact_id = -1
      WHERE EXISTS (SELECT 'Y'
                    FROM sequencing_sample_fact ssf, im_sequencing_sample_fact issf
                    WHERE (  ssf.sequencing_run_id = issf.sequencing_run_id
                             AND ssf.etl_date > issf.etl_date )
                          OR (  ssf.flowcell_barcode = issf.flowcell_barcode
                                AND ssf.sequencing_run_id > issf.sequencing_run_id
                                AND ssf.etl_date > issf.etl_date ));

      -- Delete them from fact table
      DELETE FROM sequencing_sample_fact
      WHERE sequencing_sample_fact_id IN
            (SELECT ssf.sequencing_sample_fact_id
             FROM sequencing_sample_fact ssf, im_sequencing_sample_fact issf
             WHERE (  ssf.sequencing_run_id = issf.sequencing_run_id
                      AND ssf.etl_date <= issf.etl_date )
                   OR (  ssf.flowcell_barcode = issf.flowcell_barcode
                         AND ssf.sequencing_run_id <= issf.sequencing_run_id
                         AND ssf.etl_date <= issf.etl_date ));
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' sequencing_sample_fact rows' );

      -- Now delete the import rows to ignore
      DELETE FROM im_sequencing_sample_fact WHERE sequencing_sample_fact_id = -1;

      COMMIT;

    END DO_DELETES;

  PROCEDURE MERGE_RESEARCH_PROJECT
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_research_project
                  WHERE is_delete = 'F')
      LOOP
        BEGIN
          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM research_project
          WHERE research_project_id = new.research_project_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE research_project
            SET current_status = new.current_status,
              created_date = new.created_date,
              title = new.title,
              irb_not_engaged = new.irb_not_engaged,
              jira_ticket_key = new.jira_ticket_key,
              parent_research_project_id = new.parent_research_project_id,
              root_research_project_id = new.root_research_project_id,
              etl_date = new.etl_date
            WHERE research_project_id = new.research_project_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;
          ELSIF V_LATEST_ETL_DATE IS NULL THEN
            INSERT INTO research_project (
              research_project_id,
              current_status,
              created_date,
              title,
              irb_not_engaged,
              jira_ticket_key,
              parent_research_project_id,
              root_research_project_id,
              etl_date
            ) VALUES (
              new.research_project_id,
              new.current_status,
              new.created_date,
              new.title,
              new.irb_not_engaged,
              new.jira_ticket_key,
              new.parent_research_project_id,
              new.root_research_project_id,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
          END IF;

          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_research_project.dat line ' || new.line_number || '  ' ||
              errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'research_project' );
    END MERGE_RESEARCH_PROJECT;

  PROCEDURE MERGE_PRICE_ITEM
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_price_item
                  WHERE is_delete = 'F') LOOP
        BEGIN
          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM price_item
          WHERE price_item_id = new.price_item_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE price_item
            SET platform = new.platform,
              category = new.category,
              price_item_name = new.price_item_name,
              quote_server_id = new.quote_server_id,
              price = new.price,
              units = new.units,
              etl_date = new.etl_date
            WHERE price_item_id = new.price_item_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;
          ELSIF V_LATEST_ETL_DATE IS NULL THEN
            INSERT INTO price_item (
              price_item_id,
              platform,
              category,
              price_item_name,
              quote_server_id,
              price,
              units,
              etl_date
            ) VALUES (
              new.price_item_id,
              new.platform,
              new.category,
              new.price_item_name,
              new.quote_server_id,
              new.price,
              new.units,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
          END IF;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_price_item.dat line ' || new.line_number || '  ' || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'price_item' );
    END MERGE_PRICE_ITEM;

  PROCEDURE MERGE_PRODUCT
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN (SELECT
                    *
                  FROM im_product
                  WHERE is_delete = 'F') LOOP
        BEGIN
          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM product
          WHERE product_id = new.product_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE product
            SET product_name  = new.product_name,
              part_number  = new.part_number,
              availability_date = new.availability_date,
              discontinued_date = new.discontinued_date,
              expected_cycle_time_sec = new.expected_cycle_time_sec,
              guaranteed_cycle_time_sec = new.guaranteed_cycle_time_sec,
              samples_per_week = new.samples_per_week,
              is_top_level_product = new.is_top_level_product,
              workflow_name = new.workflow_name,
              product_family_name = new.product_family_name,
              primary_price_item_id = new.primary_price_item_id,
              aggregation_data_type = new.aggregation_data_type,
              external_only_product = new.external_only_product,
              saved_in_sap = new.saved_in_sap,
              etl_date = new.etl_date
            WHERE product_id = new.product_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

          ELSIF V_LATEST_ETL_DATE IS NULL THEN

            INSERT INTO product (
              product_id,
              product_name,
              part_number,
              availability_date,
              discontinued_date,
              expected_cycle_time_sec,
              guaranteed_cycle_time_sec,
              samples_per_week,
              is_top_level_product,
              workflow_name,
              product_family_name,
              primary_price_item_id,
              aggregation_data_type,
              external_only_product,
              saved_in_sap,
              etl_date
            ) VALUES (
              new.product_id,
              new.product_name,
              new.part_number,
              new.availability_date,
              new.discontinued_date,
              new.expected_cycle_time_sec,
              new.guaranteed_cycle_time_sec,
              new.samples_per_week,
              new.is_top_level_product,
              new.workflow_name,
              new.product_family_name,
              new.primary_price_item_id,
              new.aggregation_data_type,
              new.external_only_product,
              new.saved_in_sap,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
          END IF;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product.dat line ' || new.line_number || '  ' || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'product' );
    END MERGE_PRODUCT;

  PROCEDURE MERGE_LAB_VESSEL
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_lab_vessel
                  WHERE is_delete = 'F') LOOP
        BEGIN
          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM lab_vessel
          WHERE lab_vessel_id = new.lab_vessel_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE lab_vessel
            SET lab_vessel_id = new.lab_vessel_id,
              label = new.label,
              lab_vessel_type = new.lab_vessel_type,
              name = new.name,
              created_on = new.created_on,
              etl_date = new.etl_date
            WHERE lab_vessel_id = new.lab_vessel_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;
          ELSIF V_LATEST_ETL_DATE IS NULL THEN

            INSERT INTO lab_vessel (
              lab_vessel_id,
              label,
              lab_vessel_type,
              name,
              created_on,
              etl_date
            ) VALUES (
              new.lab_vessel_id,
              new.label,
              new.lab_vessel_type,
              new.name,
              new.created_on,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
          END IF;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_lab_vessel.dat line ' || new.line_number || '  ' || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'lab_vessel' );
    END MERGE_LAB_VESSEL;

  PROCEDURE MERGE_ABANDON_VESSEL
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN (SELECT line_number,
                    etl_date,
                    abandon_id,
                    abandon_vessel_id,
                    reason,
                    abandoned_on,
                    vessel_position
                  FROM im_abandon_vessel
                  WHERE is_delete = 'F' ) LOOP
        BEGIN
          SELECT MAX(etl_date)
          INTO V_LATEST_ETL_DATE
          FROM abandon_vessel
          WHERE abandon_id = new.abandon_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE abandon_vessel
            SET abandon_vessel_id = new.abandon_vessel_id,
              reason = new.reason,
              abandoned_on = new.abandoned_on,
              vessel_position = new.vessel_position,
              etl_date = new.etl_date
            WHERE abandon_id = new.abandon_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

          ELSIF V_LATEST_ETL_DATE IS NULL THEN

            INSERT INTO abandon_vessel (
              abandon_type,
              abandon_id,
              abandon_vessel_id,
              vessel_position,
              reason,
              abandoned_on,
              etl_date
            ) VALUES (
              CASE WHEN new.vessel_position IS NULL THEN 'AbandonVessel' ELSE 'AbandonVesselPosition' END,
              new.abandon_id,
              new.abandon_vessel_id,
              new.vessel_position,
              new.reason,
              new.abandoned_on,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
          END IF;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
                TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_abandon_vessel.dat line ' || new.line_number || '  ' || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'abandon_vessel' );
    END MERGE_ABANDON_VESSEL;

  PROCEDURE MERGE_LAB_METRIC
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;

      FOR new IN (SELECT * FROM im_lab_metric WHERE is_delete = 'F') LOOP
        BEGIN

          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM lab_metric
          WHERE lab_metric_id = new.lab_metric_id;

          -- Do an update only if this ETL date greater than what's in DB already (unlikely fix-up test)
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE lab_metric
            SET quant_type      = new.quant_type,
              quant_units     = new.quant_units,
              quant_value     = new.quant_value,
              run_name        = new.run_name,
              run_date        = new.run_date,
              lab_vessel_id   = new.lab_vessel_id,
              vessel_barcode  = new.vessel_barcode,
              rack_position   = new.rack_position,
              decision        = new.decision,
              decision_date   = new.decision_date,
              decider         = new.decider,
              override_reason = new.override_reason,
              etl_date        = new.etl_date
            WHERE lab_metric_id   = new.lab_metric_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

          ELSIF V_LATEST_ETL_DATE IS NULL THEN
            INSERT INTO lab_metric (
              lab_metric_id,
              quant_type, quant_units, quant_value,
              run_name, run_date,
              lab_vessel_id, vessel_barcode, rack_position,
              decision, decision_date, decider,
              override_reason, etl_date )
              VALUES( new.lab_metric_id,
                new.quant_type, new.quant_units, new.quant_value,
                new.run_name, new.run_date,
                new.lab_vessel_id, new.vessel_barcode, new.rack_position,
                new.decision, new.decision_date, new.decider,
                new.override_reason, new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;

            -- ELSE ignore older ETL extract
          END IF;

          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_lab_metric.dat line ' || new.line_number || '  ' || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'lab_metric' );
    END MERGE_LAB_METRIC;

  PROCEDURE MERGE_WORKFLOW
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_workflow
                  WHERE is_delete = 'F') LOOP
        BEGIN
          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM workflow
          WHERE workflow_id = new.workflow_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE workflow
            SET workflow_id = new.workflow_id,
              workflow_name = new.workflow_name,
              workflow_version = new.workflow_version,
              etl_date = new.etl_date
            WHERE workflow_id = new.workflow_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;
          ELSIF V_LATEST_ETL_DATE IS NULL THEN
            INSERT INTO workflow (
              workflow_id,
              workflow_name,
              workflow_version,
              etl_date
            ) VALUES (
              new.workflow_id,
              new.workflow_name,
              new.workflow_version,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
            -- ELSE ignore older ETL extract
          END IF;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_workflow.dat line ' || new.line_number || '  ' || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'workflow' );
    END MERGE_WORKFLOW;

  PROCEDURE MERGE_WORKFLOW_PROCESS
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_workflow_process
                  WHERE is_delete = 'F') LOOP
        BEGIN
          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM workflow_process
          WHERE process_id = new.process_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE workflow_process
            SET process_id = new.process_id,
              process_name = new.process_name,
              process_version = new.process_version,
              step_name = new.step_name,
              event_name = new.event_name,
              etl_date = new.etl_date
            WHERE process_id = new.process_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;
          ELSIF V_LATEST_ETL_DATE IS NULL THEN
            INSERT INTO workflow_process (
              process_id,
              process_name,
              process_version,
              step_name,
              event_name,
              etl_date
            ) VALUES (
              new.process_id,
              new.process_name,
              new.process_version,
              new.step_name,
              new.event_name,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
            -- ELSE ignore older ETL extract
          END IF;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_workflow_process.dat line ' || new.line_number || '  ' ||
              errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'workflow_process' );
    END MERGE_WORKFLOW_PROCESS;

  PROCEDURE MERGE_SEQUENCING_RUN
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_sequencing_run
                  WHERE is_delete = 'F') LOOP
        BEGIN
          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM sequencing_run
          WHERE sequencing_run_id = new.sequencing_run_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE sequencing_run
            SET run_name = new.run_name,
              barcode = new.barcode,
              registration_date = new.registration_date,
              instrument = new.instrument,
              setup_read_structure = new.setup_read_structure,
              actual_read_structure = new.actual_read_structure,
              etl_date = new.etl_date
            WHERE sequencing_run_id = new.sequencing_run_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;
          ELSIF V_LATEST_ETL_DATE IS NULL THEN
            INSERT INTO sequencing_run (
              sequencing_run_id,
              run_name,
              barcode,
              registration_date,
              instrument,
              setup_read_structure,
              actual_read_structure,
              etl_date
            ) VALUES (
              new.sequencing_run_id,
              new.run_name,
              new.barcode,
              new.registration_date,
              new.instrument,
              new.setup_read_structure,
              new.actual_read_structure,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
            -- ELSE ignore older ETL extract
          END IF;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_sequencing_run.dat line ' || new.line_number || '  ' ||
              errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'sequencing_run' );
    END MERGE_SEQUENCING_RUN;

  PROCEDURE MERGE_REGULATORY_INFO
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      -- Regulatory info data
      FOR new IN ( SELECT *
                   FROM im_regulatory_info
                   WHERE is_delete = 'F' ) LOOP
        BEGIN
          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM regulatory_info
          WHERE regulatory_info_id = new.regulatory_info_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE regulatory_info
            SET regulatory_info_id = new.regulatory_info_id,
              identifier = new.identifier,
              type       = new.type,
              name       = new.name,
              etl_date   = new.etl_date
            WHERE regulatory_info_id = new.regulatory_info_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;
          ELSIF V_LATEST_ETL_DATE IS NULL THEN
            INSERT INTO regulatory_info (
              regulatory_info_id,
              identifier,
              type,
              name,
              etl_date
            ) VALUES (
              new.regulatory_info_id,
              new.identifier,
              new.type,
              new.name,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
            -- ELSE ignore older ETL extract
          END IF;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_regulatory_info.dat line ' || new.line_number || '  ' || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'regulatory_info' );
    END MERGE_REGULATORY_INFO;

  PROCEDURE MERGE_PRODUCT_ORDER
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_REF_INS_COUNT PLS_INTEGER;
    V_REF_DEL_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      V_REF_INS_COUNT := 0;
      V_REF_DEL_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_product_order
                  WHERE is_delete = 'F') LOOP
        BEGIN
          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM product_order
          WHERE product_order_id = new.product_order_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE product_order
            SET research_project_id = new.research_project_id,
              product_id = new.product_id,
              status = new.status,
              created_date = new.created_date,
              modified_date = new.modified_date,
              title = new.title,
              quote_id = new.quote_id,
              jira_ticket_key = new.jira_ticket_key,
              owner = new.owner,
              placed_date = new.placed_date,
              skip_regulatory_reason = new.skip_regulatory_reason,
              sap_order_number = new.sap_order_number,
              array_chip_type = new.array_chip_type,
              call_rate_threshold = new.call_rate_threshold,
              etl_date = new.etl_date
            WHERE product_order_id = new.product_order_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

          ELSIF V_LATEST_ETL_DATE IS NULL THEN
            INSERT INTO product_order (
              product_order_id,
              research_project_id,
              product_id,
              status,
              created_date,
              modified_date,
              title,
              quote_id,
              jira_ticket_key,
              owner,
              placed_date,
              skip_regulatory_reason,
              sap_order_number,
              array_chip_type,
              call_rate_threshold,
              etl_date
            ) VALUES (
              new.product_order_id,
              new.research_project_id,
              new.product_id,
              new.status,
              new.created_date,
              new.modified_date,
              new.title,
              new.quote_id,
              new.jira_ticket_key,
              new.owner,
              new.placed_date,
              new.skip_regulatory_reason,
              new.sap_order_number,
              new.array_chip_type,
              new.call_rate_threshold,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
            -- ELSE ignore older ETL extract
          END IF ;

          IF new.etl_date > V_LATEST_ETL_DATE OR V_LATEST_ETL_DATE IS NULL THEN
            -- Wrap this in a nested error handler to record any pdo_regulatory_infos error
            BEGIN
              -- Replace pdo_regulatory_infos many-to-many table
              -- Flush PDO regulatory info for all changed/deleted product orders
              DELETE FROM pdo_regulatory_infos
              WHERE product_order = new.product_order_id;

              V_REF_DEL_COUNT := V_REF_DEL_COUNT + SQL%ROWCOUNT;
              V_PK_ARR := PKS_FROM_JAVA_ARRAYSTOSTRING( new.reg_info_ids );
              IF V_PK_ARR.COUNT > 0 THEN
                FOR V_INDEX IN V_PK_ARR.FIRST .. V_PK_ARR.LAST
                LOOP
                  INSERT INTO pdo_regulatory_infos ( product_order, regulatory_infos, etl_date )
                  VALUES ( new.product_order_id, V_PK_ARR(V_INDEX), new.etl_date );
                  V_REF_INS_COUNT := V_REF_INS_COUNT + SQL%ROWCOUNT;
                END LOOP;
              END IF;
              EXCEPTION WHEN OTHERS THEN
              errmsg := SQLERRM;
              DBMS_OUTPUT.PUT_LINE(
                  TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order.dat line, pdo_regulatory_infos data '
                  || new.line_number || '  ' || errmsg);
            END;
          END IF;

          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order.dat line ' || new.line_number || '  ' || errmsg);
          CONTINUE;
        END;
      END LOOP;

      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'product_order' );
      DBMS_OUTPUT.PUT_LINE( 'Deleted ' || V_REF_DEL_COUNT || ' pdo_regulatory_infos rows' );
      SHOW_ETL_STATS(  0, V_REF_INS_COUNT, 'pdo_regulatory_infos' );

    END MERGE_PRODUCT_ORDER;

  PROCEDURE MERGE_PRODUCT_ORDER_ADD_ON
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_product_order_add_on
                  WHERE is_delete = 'F') LOOP
        BEGIN
          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM product_order_add_on
          WHERE product_order_add_on_id = new.product_order_add_on_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE product_order_add_on
            SET product_order_id = new.product_order_id,
              product_id = new.product_id,
              etl_date = new.etl_date
            WHERE product_order_add_on_id = new.product_order_add_on_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;
          ELSIF V_LATEST_ETL_DATE IS NULL THEN
            INSERT INTO product_order_add_on (
              product_order_add_on_id,
              product_order_id,
              product_id,
              etl_date
            ) VALUES (
              new.product_order_add_on_id,
              new.product_order_id,
              new.product_id,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
            -- ELSE ignore older ETL extract
          END IF;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order_add_on.dat line ' || new.line_number || '  ' ||
              errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'product_order_add_on' );
    END MERGE_PRODUCT_ORDER_ADD_ON;

  PROCEDURE MERGE_RESEARCH_PROJECT_STATUS
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_PREV_STATUS RESEARCH_PROJECT_STATUS.STATUS%TYPE;
    V_PREV_STATUS_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_research_project_status
                  WHERE is_delete = 'F') LOOP
        BEGIN
          -- VERY unlikely unless same file reloaded
          UPDATE research_project_status
          SET status_date = new.status_date,
            status = new.status,
            etl_date = new.etl_date
          WHERE research_project_id = new.research_project_id
                AND status_date = new.status_date
                AND etl_date < new.etl_date;

          V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

          -- If same record reloaded and updated, don't bother with additional logic
          IF SQL%ROWCOUNT > 0 THEN
            CONTINUE;
          END IF;

          -- ELSE do an insert only if status has changed since last ETL
          -- (always returns a row, null value if no data found)
          -- TODO: JMS Clean up multiple (70+ in one case) status rows with no change, keep oldest changed status
          SELECT MAX( status ) KEEP ( DENSE_RANK LAST ORDER BY status_date ASC )
            , MAX( status_date ) KEEP ( DENSE_RANK LAST ORDER BY status_date ASC )
          INTO V_PREV_STATUS, V_PREV_STATUS_DATE
          FROM research_project_status
          WHERE research_project_id = new.research_project_id;

          IF V_PREV_STATUS IS NULL OR ( V_PREV_STATUS <> new.status AND V_PREV_STATUS_DATE < new.status_date ) THEN
            INSERT INTO research_project_status (
              research_project_id,
              status_date,
              status,
              etl_date
            ) VALUES (
              new.research_project_id,
              new.status_date,
              new.status,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
          END IF;

          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_research_project_status.dat line ' || new.line_number || '  '
              || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'research_project_status' );
    END MERGE_RESEARCH_PROJECT_STATUS;


  PROCEDURE MERGE_RESEARCH_PROJECT_PERSON
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_research_project_person
                  WHERE is_delete = 'F') LOOP
        BEGIN
          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM research_project_person
          WHERE research_project_person_id = new.research_project_person_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE research_project_person
            SET research_project_id = new.research_project_id,
              project_role = new.project_role,
              person_id = new.person_id,
              first_name = new.first_name,
              last_name = new.last_name,
              username = new.username,
              etl_date = new.etl_date
            WHERE research_project_person_id = new.research_project_person_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;
          ELSIF V_LATEST_ETL_DATE IS NULL THEN
            INSERT INTO research_project_person (
              research_project_person_id,
              research_project_id,
              project_role,
              person_id,
              first_name,
              last_name,
              username,
              etl_date
            ) VALUES (
              new.research_project_person_id,
              new.research_project_id,
              new.project_role,
              new.person_id,
              new.first_name,
              new.last_name,
              new.username,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
            -- ELSE ignore older ETL extract
          END IF;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_research_project_person.dat line ' || new.line_number || '  '
              || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'research_project_person' );
    END MERGE_RESEARCH_PROJECT_PERSON;


  PROCEDURE MERGE_RESEARCH_PROJECT_FUNDING
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_research_project_funding
                  WHERE is_delete = 'F') LOOP
        BEGIN
          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM research_project_funding
          WHERE research_project_funding_id = new.research_project_funding_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE research_project_funding
            SET funding_id = new.funding_id,
              etl_date = new.etl_date
            WHERE research_project_funding_id = new.research_project_funding_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;
          ELSIF V_LATEST_ETL_DATE IS NULL THEN
            INSERT INTO research_project_funding (
              research_project_id,
              research_project_funding_id,
              funding_id,
              etl_date
            ) VALUES (
              new.research_project_id,
              new.research_project_funding_id,
              new.funding_id,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
            -- ELSE ignore older ETL extract
          END IF;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_research_project_funding.dat line ' || new.line_number || '  '
              || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'research_project_funding' );
    END MERGE_RESEARCH_PROJECT_FUNDING;


  PROCEDURE MERGE_RESEARCH_PROJECT_COHORT
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_research_project_cohort
                  WHERE is_delete = 'F') LOOP
        BEGIN
          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM research_project_cohort
          WHERE research_project_cohort_id = new.research_project_cohort_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            -- Is this ever updated?  Delete then insert is the likely scenario
            UPDATE research_project_cohort
            SET research_project_id = new.research_project_id,
              etl_date = new.etl_date
            WHERE research_project_cohort_id = new.research_project_cohort_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;
          ELSIF V_LATEST_ETL_DATE IS NULL THEN
            INSERT INTO research_project_cohort (
              research_project_id,
              research_project_cohort_id,
              etl_date
            ) VALUES (
              new.research_project_id,
              new.research_project_cohort_id,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
            -- ELSE ignore older ETL extract
          END IF;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_research_project_cohort.dat line ' || new.line_number || '  '
              || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'research_project_cohort' );
    END MERGE_RESEARCH_PROJECT_COHORT;


  PROCEDURE MERGE_RESEARCH_PROJECT_IRB
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_research_project_irb
                  WHERE is_delete = 'F') LOOP
        BEGIN
          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM research_project_irb
          WHERE research_project_irb_id = new.research_project_irb_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE research_project_irb
            SET research_project_id = new.research_project_id,
              research_project_irb = new.research_project_irb,
              research_project_irb_type = new.research_project_irb_type,
              etl_date = new.etl_date
            WHERE research_project_irb_id = new.research_project_irb_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;
          ELSIF V_LATEST_ETL_DATE IS NULL THEN
            INSERT INTO research_project_irb (
              research_project_irb_id,
              research_project_id,
              research_project_irb,
              research_project_irb_type,
              etl_date
            ) VALUES (
              new.research_project_irb_id,
              new.research_project_id,
              new.research_project_irb,
              new.research_project_irb_type,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
            -- ELSE ignore older ETL extract
          END IF;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_research_project_irb.dat line ' || new.line_number || '  ' ||
              errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'research_project_irb' );
    END MERGE_RESEARCH_PROJECT_IRB;


  PROCEDURE MERGE_PRODUCT_ORDER_SAMPLE
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_product_order_sample
                  WHERE is_delete = 'F') LOOP
        BEGIN
          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM product_order_sample
          WHERE product_order_sample_id = new.product_order_sample_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE product_order_sample
            SET product_order_id = new.product_order_id,
              sample_name = new.sample_name,
              delivery_status = new.delivery_status,
              sample_position = new.sample_position,
              PARTICIPANT_ID = new.PARTICIPANT_ID,
              SAMPLE_TYPE = new.SAMPLE_TYPE,
              SAMPLE_RECEIPT = new.SAMPLE_RECEIPT,
              ORIGINAL_SAMPLE_TYPE = new.ORIGINAL_SAMPLE_TYPE,
              etl_date = new.etl_date
            WHERE product_order_sample_id = new.product_order_sample_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

          ELSIF V_LATEST_ETL_DATE IS NULL THEN
            INSERT INTO product_order_sample (
              product_order_sample_id,
              product_order_id,
              sample_name,
              delivery_status,
              sample_position,
              PARTICIPANT_ID,
              SAMPLE_TYPE,
              SAMPLE_RECEIPT,
              ORIGINAL_SAMPLE_TYPE,
              etl_date,
              billing_etl_date,
              risk_etl_date
            ) VALUES (
              new.product_order_sample_id,
              new.product_order_id,
              new.sample_name,
              new.delivery_status,
              new.sample_position,
              new.PARTICIPANT_ID,
              new.SAMPLE_TYPE,
              new.SAMPLE_RECEIPT,
              new.ORIGINAL_SAMPLE_TYPE,
              new.etl_date,
              new.etl_date,
              new.etl_date);

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
            -- ELSE ignore older ETL extract
          END IF;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order_sample.dat line ' || new.line_number || '  ' ||
              errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'product_order_sample' );
    END MERGE_PRODUCT_ORDER_SAMPLE;


  PROCEDURE MERGE_EVENT_FACT
  IS
    V_INS_COUNT PLS_INTEGER;

    -- There are cases where a fix-up can result in multiple rows in the same file
    -- (unique by lab_event_id, ef.sample_name, lcset_sample_name, and ef.position)
    cursor cur_dups is
      SELECT max(rowid) as rowtokeep, min(line_number) as line_number
        FROM im_event_fact
       WHERE is_delete = 'F'
      GROUP BY lab_event_id, sample_name, lcset_sample_name, position;

    type rowid_arr_ty is table of rowid index by pls_integer;
    v_rowid_arr rowid_arr_ty;

    type nbr_arr_ty is table of number(10) index by pls_integer;
    v_nbr_arr nbr_arr_ty;

    v_rowid rowid;
    V_IDX pls_integer;

    v_etldate DATE;

    BEGIN
      V_INS_COUNT := 0;

      OPEN cur_dups;
      FETCH cur_dups BULK COLLECT INTO v_rowid_arr, v_nbr_arr;
      CLOSE cur_dups;

      IF v_rowid_arr.count > 0 THEN
        -- Only one batch per run - all dates (should be) the same
        SELECT ETL_DATE INTO v_etldate FROM im_event_fact WHERE ROWNUM < 2;

        -- TODO Bulk collect errors in a single FORALL statement?
        FOR V_IDX IN v_rowid_arr.FIRST .. v_rowid_arr.LAST LOOP
        BEGIN
          v_rowid := v_rowid_arr(V_IDX);
          -- All older ETL records deleted from import tables, inserts only
          INSERT INTO event_fact (
            event_fact_id, lab_event_id, workflow_id,
            process_id, lab_event_type, product_order_id,
            sample_name, lcset_sample_name, batch_name,
            station_name, lab_vessel_id, position,
            event_date, etl_date, program_name,
            molecular_indexing_scheme, library_name
          ) SELECT event_fact_id_seq.nextval, lab_event_id, workflow_id,
                   process_id, lab_event_type, product_order_id,
                   sample_name, lcset_sample_name, batch_name,
                   station_name, lab_vessel_id, position,
                   event_date, etl_date, program_name,
                   molecular_indexing_scheme, library_name
              from IM_EVENT_FACT
             where rowid = v_rowid;

          V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
        EXCEPTION
          WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(v_etldate, 'YYYYMMDDHH24MISS') || '_event_fact.dat line ' || v_nbr_arr(V_IDX) || '  ' || errmsg);
          CONTINUE;
        END;
        END LOOP;
      END IF;
      SHOW_ETL_STATS(  0, V_INS_COUNT, 'event_fact' );
    END MERGE_EVENT_FACT;


  PROCEDURE MERGE_ANCESTRY
  IS
    V_ANCEST_INS_COUNT PLS_INTEGER;
    -- Nothing ever updated
    V_UPD_COUNT CONSTANT PLS_INTEGER := 0;
    BEGIN
      V_ANCEST_INS_COUNT := 0;
      -- All older ETL records deleted from import tables, inserts only
      -- De-duplicate in cases where a fixup may put duplicated events in a single file
      FOR new IN (SELECT DISTINCT ancestor_event_id, ancestor_library_id, ancestor_library_type,
                         ancestor_library_creation, child_event_id, child_library_id,
                         child_library_type, child_library_creation, etl_date
                    FROM im_library_ancestry
                   WHERE is_delete = 'F') LOOP
        BEGIN
          INSERT INTO library_ancestry (
            ancestor_event_id, ancestor_library_id, ancestor_library_type, ancestor_library_creation,
            child_event_id, child_library_id, child_library_type, child_library_creation,
            etl_date )
            SELECT
              new.ancestor_event_id, new.ancestor_library_id, new.ancestor_library_type, new.ancestor_library_creation,
              new.child_event_id, new.child_library_id, new.child_library_type, new.child_library_creation,
              new.etl_date
            FROM DUAL WHERE NOT EXISTS (
                SELECT 'Y' FROM library_ancestry
                WHERE ancestor_library_id = new.ancestor_library_id
                      AND child_library_id    = new.child_library_id  );
          V_ANCEST_INS_COUNT := V_ANCEST_INS_COUNT + SQL%ROWCOUNT;

          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_library_ancestry.dat ancestor/child event ' || new.ancestor_event_id || ' / ' || new.child_event_id || ' : ' || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_ANCEST_INS_COUNT, 'library_ancestry' );
    END MERGE_ANCESTRY;

  /*
   * Links vessel barcodes (libraries) in the library ancestry table to event-vessel combination in event_fact table
   * See RPT-3119/GPLIM-3927
   */
  PROCEDURE MERGE_LIBRARY_SAMPLE
  IS
    V_LIBRARY_INS_COUNT PLS_INTEGER;
    -- Nothing ever updated
    V_UPD_COUNT CONSTANT PLS_INTEGER := 0;

    V_LIBRARY_LABEL LAB_VESSEL.LABEL%TYPE;
    V_LIBRARY_DATE LAB_VESSEL.CREATED_ON%TYPE;

    BEGIN
      V_LIBRARY_INS_COUNT := 0;
      FOR new IN (SELECT lab_vessel_id as library_id
                       , lab_event_id as event_id
                       , library_name as library_type
                       , event_date as library_creation
                       , MIN(line_number) as line_number
                       , etl_date
                    FROM im_event_fact
                   WHERE library_name IS NOT NULL
                     AND is_delete = 'F'
                  GROUP BY lab_vessel_id, lab_event_id, library_name, event_date, etl_date
      ) LOOP
        BEGIN

          -- Rows for libraries - we've already deleted any rows related to libraries
          INSERT INTO LIBRARY_LCSET_SAMPLE_BASE (
            LIBRARY_LABEL, LIBRARY_ID,
            LIBRARY_TYPE, LIBRARY_CREATION_DATE, LIBRARY_EVENT_ID )
          ( SELECT label, lab_vessel_id, new.library_type
                 , created_on, new.event_id
              FROM lab_vessel
             WHERE lab_vessel_id = new.library_id );

          V_LIBRARY_INS_COUNT := V_LIBRARY_INS_COUNT + SQL%ROWCOUNT;

          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE( 'The following error is from inserting library_ancestry data into LIBRARY_LCSET_SAMPLE_BASE, the line number is irrelevant');
          DBMS_OUTPUT.PUT_LINE( 'Library ID: ' || new.library_id || ', Library Type: ' || new.library_type || ', Event ID: ' || new.event_id );
          DBMS_OUTPUT.PUT_LINE( TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_library_ancestry.dat line ' || new.line_number || '  ' || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_LIBRARY_INS_COUNT, 'library_lcset_sample_base (from library ancestry)' );
    END MERGE_LIBRARY_SAMPLE;

  /*
   * Builds out events in a horizontal row for each sample in an Infinium process flow
   *    from DNA plate to amp plate to chip well
   * Does not deal with event deletions because no event type is available for deleted rows and otherwise would have to
   *    scan 14 separate event IDs horizontally in each row for every event deleted to clear any (rarely) deleted data
   */
  PROCEDURE MERGE_ARRAY_PROCESS_FLOW
  IS
    V_COUNT     PLS_INTEGER;
    V_THE_ROWID UROWID;
    V_LABEL VARCHAR2(255);

    -- Flag to split row and chip barcode if DNA/Amp plate well is re-hybed to a new chip
    V_DO_SPLIT  BOOLEAN;
    V_CHIP_BARCODE ARRAY_PROCESS_FLOW.CHIP%TYPE;

    V_ROWID_ARR DBMS_SQL.UROWID_TABLE;

    BEGIN
      V_COUNT := 0;

      FOR new IN (
      SELECT LINE_NUMBER, ETL_DATE,
        product_order_id, batch_name, lcset_sample_name, sample_name,
        lab_event_id, lab_event_type, station_name, event_date,
        lab_vessel_id, position,
        'N' as split_on_rehyb
      FROM im_array_process
      WHERE is_delete = 'F'
      UNION ALL
      SELECT LINE_NUMBER, ETL_DATE,
        product_order_id, batch_name, lcset_sample_name, sample_name,
        lab_event_id, lab_event_type, station_name, event_date,
        lab_vessel_id, position,
        'N' as split_on_rehyb
      FROM im_event_fact
      WHERE is_delete = 'F'
            AND product_order_id  IS NOT NULL
            AND lcset_sample_name IS NOT NULL
            AND NVL(batch_name, 'NONE') <> 'NONE'
            AND lab_event_type IN (
        'InfiniumAmplification',
        'InfiniumPostFragmentationHybOvenLoaded',
        'InfiniumFragmentation',
        'InfiniumPrecipitation',
        'InfiniumPostPrecipitationHeatBlockLoaded',
        'InfiniumPrecipitationIsopropanolAddition',
        'InfiniumResuspension',
        'InfiniumPostResuspensionHybOven' )
      UNION ALL
      SELECT LINE_NUMBER, ETL_DATE,
        product_order_id, batch_name, lcset_sample_name, sample_name,
        lab_event_id, lab_event_type, station_name, event_date,
        lab_vessel_id, position,
        'Y' as split_on_rehyb
      FROM im_event_fact
      WHERE is_delete = 'F'
            AND product_order_id  IS NOT NULL
            AND lcset_sample_name IS NOT NULL
            AND NVL(batch_name, 'NONE') <> 'NONE'
            AND lab_event_type IN (
        'InfiniumHybridization',
        'InfiniumPostHybridizationHybOvenLoaded',
        'InfiniumHybChamberLoaded',
        'InfiniumXStain',
        'InfiniumAutocallSomeStarted',
        'InfiniumAutoCallAllStarted' ) )
      LOOP
        -- Find initial base row (multiple if chip rehyb)
        BEGIN

          SELECT ROWID
          BULK COLLECT INTO V_ROWID_ARR
          FROM array_process_flow
          WHERE product_order_id  = new.product_order_id
                AND lcset_sample_name = new.lcset_sample_name
                AND batch_name = new.batch_name;

          IF V_ROWID_ARR.COUNT = 0 THEN
            -- Have to create initial base row data
            INSERT INTO array_process_flow (
              product_order_id, batch_name, lcset_sample_name, sample_name, etl_mod_timestamp )
            VALUES( new.product_order_id, new.batch_name, new.lcset_sample_name, new.sample_name, V_ETL_MOD_TIMESTAMP )
            RETURNING ROWID INTO V_THE_ROWID;
            -- Reassign for updates
            V_ROWID_ARR(1) := V_THE_ROWID;

          ELSIF new.split_on_rehyb = 'Y' AND V_ROWID_ARR.COUNT > 0 THEN
          
            V_DO_SPLIT := FALSE;
            -- Does a row match chip label?
            SELECT LABEL INTO V_CHIP_BARCODE FROM LAB_VESSEL WHERE LAB_VESSEL_ID = new.LAB_VESSEL_ID;

            BEGIN
              SELECT ROWID INTO V_THE_ROWID
              FROM array_process_flow
              WHERE product_order_id  = new.product_order_id
                    AND lcset_sample_name = new.lcset_sample_name
                    AND batch_name = new.batch_name
                    AND NVL(chip, V_CHIP_BARCODE)  = V_CHIP_BARCODE
                    AND ROWNUM = 1;
              -- Reassign for updates
              V_ROWID_ARR.DELETE;
              V_ROWID_ARR(1) := V_THE_ROWID;
              EXCEPTION WHEN NO_DATA_FOUND THEN
              V_DO_SPLIT := TRUE;
            END;

            IF V_DO_SPLIT THEN
              -- Row to update
              V_THE_ROWID := V_ROWID_ARR(1);
              V_ROWID_ARR.DELETE;
              V_ROWID_ARR(1) := V_THE_ROWID;

              -- Copy all existing data to new row (and ignore)
              INSERT INTO array_process_flow
                SELECT *
                FROM array_process_flow
                WHERE ROWID = V_THE_ROWID;

              -- Clear out post hyb data from row
              UPDATE ARRAY_PROCESS_FLOW
              SET HYB_EVENT_ID = NULL,
                HYB_STATION = NULL,
                HYB_POSITION = NULL,
                HYB_DATE = NULL,
                CHIP = NULL,
                CHIP_WELL_BARCODE = NULL,
                POSTHYBHYBOVENLOADED_EVENT_ID = NULL,
                POSTHYBHYBOVENLOADED_STATION = NULL,
                POSTHYBHYBOVENLOADED_DATE = NULL,
                HYBCHAMBERLOADED_EVENT_ID = NULL,
                HYBCHAMBERLOADED = NULL,
                HYBCHAMBERLOADED_DATE = NULL,
                XSTAIN_EVENT_ID = NULL,
                XSTAIN = NULL,
                XSTAIN_DATE = NULL,
                AUTOCALL_EVENT_ID = NULL,
                AUTOCALL_STARTED = NULL,
                SCANNER = NULL
              WHERE ROWID = V_THE_ROWID;
            END IF;
          END IF;
        END;

        BEGIN

          -- Update applicable process flow values in base row
          CASE new.lab_event_type
            WHEN 'ArrayPlatingDilution' THEN

            FORALL IDX IN 1 .. V_ROWID_ARR.COUNT
            UPDATE array_process_flow
            SET plating_event_id = new.lab_event_id
              , plating_dilution_station = new.station_name
              , dna_plate_position = new.position
              , plating_dilution_date = new.event_date
              -- Strip position suffix from label to get plate barcode
              , dna_plate = ( SELECT REGEXP_REPLACE( LABEL, new.position || '$', '' ) FROM LAB_VESSEL WHERE LAB_VESSEL_ID = new.LAB_VESSEL_ID )
              , etl_mod_timestamp = V_ETL_MOD_TIMESTAMP
            WHERE ROWID = V_ROWID_ARR(IDX);

            -- DNA plate name is associated with plate, not plate well
            FORALL IDX IN 1 .. V_ROWID_ARR.COUNT
            UPDATE array_process_flow apf
            SET dna_plate_name = ( SELECT NAME FROM LAB_VESSEL WHERE LABEL = apf.dna_plate )
            WHERE apf.ROWID = V_ROWID_ARR(IDX);

            WHEN 'InfiniumAmplification' THEN
            FORALL IDX IN 1 .. V_ROWID_ARR.COUNT
            UPDATE array_process_flow
            SET amp_event_id = new.lab_event_id
              , amp_station = new.station_name
              , amp_plate_position = new.position
              , amp_date = new.event_date
              , amp_plate = ( SELECT LABEL FROM LAB_VESSEL WHERE LAB_VESSEL_ID = new.LAB_VESSEL_ID )
              , etl_mod_timestamp = V_ETL_MOD_TIMESTAMP
            WHERE ROWID = V_ROWID_ARR(IDX);
            WHEN 'InfiniumPostFragmentationHybOvenLoaded' THEN
            FORALL IDX IN 1 .. V_ROWID_ARR.COUNT
            UPDATE array_process_flow
            SET post_frag_event_id = new.lab_event_id
              , post_frag_hyb_oven = new.station_name
              , post_frag_hyb_oven_date = new.event_date
              , etl_mod_timestamp = V_ETL_MOD_TIMESTAMP
            WHERE ROWID = V_ROWID_ARR(IDX);
            WHEN 'InfiniumFragmentation' THEN
            FORALL IDX IN 1 .. V_ROWID_ARR.COUNT
            UPDATE array_process_flow
            SET frag_event_id = new.lab_event_id
              , frag_station = new.station_name
              , frag_date = new.event_date
              , etl_mod_timestamp = V_ETL_MOD_TIMESTAMP
            WHERE ROWID = V_ROWID_ARR(IDX);
            WHEN 'InfiniumPrecipitation' THEN
            FORALL IDX IN 1 .. V_ROWID_ARR.COUNT
            UPDATE array_process_flow
            SET precip_event_id = new.lab_event_id
              , precip_station = new.station_name
              , precip_date = new.event_date
              , etl_mod_timestamp = V_ETL_MOD_TIMESTAMP
            WHERE ROWID = V_ROWID_ARR(IDX);
            WHEN 'InfiniumPostPrecipitationHeatBlockLoaded' THEN
            FORALL IDX IN 1 .. V_ROWID_ARR.COUNT
            UPDATE array_process_flow
            SET post_precip_event_id = new.lab_event_id
              , post_precip_hyb_oven = new.station_name
              , post_precip_hyb_oven_date = new.event_date
              , etl_mod_timestamp = V_ETL_MOD_TIMESTAMP
            WHERE ROWID = V_ROWID_ARR(IDX);
            WHEN 'InfiniumPrecipitationIsopropanolAddition' THEN
            FORALL IDX IN 1 .. V_ROWID_ARR.COUNT
            UPDATE array_process_flow
            SET precip_ipa_event_id = new.lab_event_id
              , precip_ipa_station = new.station_name
              , precip_ipa_date = new.event_date
              , etl_mod_timestamp = V_ETL_MOD_TIMESTAMP
            WHERE ROWID = V_ROWID_ARR(IDX);
            WHEN 'InfiniumResuspension' THEN
            FORALL IDX IN 1 .. V_ROWID_ARR.COUNT
            UPDATE array_process_flow
            SET resuspension_event_id = new.lab_event_id
              , resuspension_station = new.station_name
              , resuspension_date = new.event_date
              , etl_mod_timestamp = V_ETL_MOD_TIMESTAMP
            WHERE ROWID = V_ROWID_ARR(IDX);
            WHEN 'InfiniumPostResuspensionHybOven' THEN
            FORALL IDX IN 1 .. V_ROWID_ARR.COUNT
            UPDATE array_process_flow
            SET postresusphyboven_event_id = new.lab_event_id
              , postresusphyboven_station = new.station_name
              , postresusphyboven_date = new.event_date
              , etl_mod_timestamp = V_ETL_MOD_TIMESTAMP
            WHERE ROWID = V_ROWID_ARR(IDX);
            -- ****** Chip events - possibly re-hyb splits ***********
            WHEN 'InfiniumHybridization' THEN
            FORALL IDX IN 1 .. V_ROWID_ARR.COUNT
            UPDATE array_process_flow
            SET hyb_event_id = new.lab_event_id
              , hyb_station = new.station_name
              , hyb_position = new.position
              , hyb_date = new.event_date
              -- Append underscore and position suffix to chip barcode to get chip well pseudo-barcode
              , ( chip, chip_well_barcode ) = ( SELECT LABEL, LABEL || '_' || new.position FROM LAB_VESSEL WHERE LAB_VESSEL_ID = new.LAB_VESSEL_ID )
              , etl_mod_timestamp = V_ETL_MOD_TIMESTAMP
            WHERE ROWID = V_ROWID_ARR(IDX);
            WHEN 'InfiniumPostHybridizationHybOvenLoaded' THEN
            FORALL IDX IN 1 .. V_ROWID_ARR.COUNT
            UPDATE array_process_flow
            SET posthybhybovenloaded_event_id = new.lab_event_id
              , posthybhybovenloaded_station = new.station_name
              , posthybhybovenloaded_date = new.event_date
              -- Append underscore and position suffix to chip barcode to get chip well pseudo-barcode
              , ( chip, chip_well_barcode ) = ( SELECT LABEL, LABEL || '_' || new.position FROM LAB_VESSEL WHERE LAB_VESSEL_ID = new.LAB_VESSEL_ID )
              , etl_mod_timestamp = V_ETL_MOD_TIMESTAMP
            WHERE ROWID = V_ROWID_ARR(IDX);
            WHEN 'InfiniumHybChamberLoaded' THEN
            FORALL IDX IN 1 .. V_ROWID_ARR.COUNT
            UPDATE array_process_flow
            SET hybchamberloaded_event_id = new.lab_event_id
              , hybchamberloaded = new.station_name
              , hybchamberloaded_date = new.event_date
              -- Append underscore and position suffix to chip barcode to get chip well pseudo-barcode
              , ( chip, chip_well_barcode ) = ( SELECT LABEL, LABEL || '_' || new.position FROM LAB_VESSEL WHERE LAB_VESSEL_ID = new.LAB_VESSEL_ID )
              , etl_mod_timestamp = V_ETL_MOD_TIMESTAMP
            WHERE ROWID = V_ROWID_ARR(IDX);
            WHEN 'InfiniumXStain' THEN
            FORALL IDX IN 1 .. V_ROWID_ARR.COUNT
            UPDATE array_process_flow
            SET xstain_event_id = new.lab_event_id
              , xstain = new.station_name
              , xstain_date = new.event_date
              -- Append underscore and position suffix to chip barcode to get chip well pseudo-barcode
              , ( chip, chip_well_barcode ) = ( SELECT LABEL, LABEL || '_' || new.position FROM LAB_VESSEL WHERE LAB_VESSEL_ID = new.LAB_VESSEL_ID )
              , etl_mod_timestamp = V_ETL_MOD_TIMESTAMP
            WHERE ROWID = V_ROWID_ARR(IDX);
            WHEN 'InfiniumAutocallSomeStarted' THEN
            FORALL IDX IN 1 .. V_ROWID_ARR.COUNT
            UPDATE array_process_flow
            SET autocall_event_id = new.lab_event_id
              , scanner = NVL(new.station_name, scanner)
              , autocall_started = new.event_date
              -- Append underscore and position suffix to chip barcode to get chip well pseudo-barcode
              , ( chip, chip_well_barcode ) = ( SELECT LABEL, LABEL || '_' || new.position FROM LAB_VESSEL WHERE LAB_VESSEL_ID = new.LAB_VESSEL_ID )
              , etl_mod_timestamp = V_ETL_MOD_TIMESTAMP
            WHERE ROWID = V_ROWID_ARR(IDX);
            WHEN  'InfiniumAutoCallAllStarted' THEN
            FORALL IDX IN 1 .. V_ROWID_ARR.COUNT
            UPDATE array_process_flow
            SET autocall_event_id = new.lab_event_id
              , scanner = NVL(new.station_name, scanner)
              , autocall_started = new.event_date
              -- Append underscore and position suffix to chip barcode to get chip well pseudo-barcode
              , ( chip, chip_well_barcode ) = ( SELECT LABEL, LABEL || '_' || new.position FROM LAB_VESSEL WHERE LAB_VESSEL_ID = new.LAB_VESSEL_ID )
              , etl_mod_timestamp = V_ETL_MOD_TIMESTAMP
            WHERE ROWID = V_ROWID_ARR(IDX)
                  AND ( autocall_event_id IS NULL
                        OR
                        -- Don't overwrite some started with all started
                        new.event_date < autocall_started );
          ELSE
            -- Shouldn't get here if event types match case list, but avoid error and don't count
            V_COUNT := V_COUNT - 1;
          END CASE;

          V_COUNT := V_COUNT + 1;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_event_fact.dat line ' || new.line_number || ' (array_process_flow merge) ' || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_COUNT, 0, ' array process flow events added' );
    END MERGE_ARRAY_PROCESS_FLOW;


  /*
   * Picks up flowcell ticket batch vessel changes for flowcell designation tickets
   * See RPT-3539/GPLIM-4136 Make Designation from Mercury available for reporting
   */
  PROCEDURE MERGE_FCT_BATCH_CRUD
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;

    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN ( SELECT line_number, batch_starting_vessel_id,
                     designation_id,
                     fct_id, fct_name, fct_type,
                     designation_library, loading_vessel, creation_date,
                     flowcell_type, lane,
                     concentration, is_pool_test,
                     etl_date
                   FROM im_fct_create
                   WHERE is_delete = 'F' )
      LOOP
        BEGIN
          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM flowcell_designation
          WHERE designation_id = new.batch_starting_vessel_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            -- Update/Insert  Note:  designation_id and batch_starting_vessel_id are synonymous
            UPDATE flowcell_designation
            SET designation_id    = new.batch_starting_vessel_id,
              fct_id              = new.fct_id,
              fct_name            = new.fct_name,
              fct_type            = new.fct_type,
              designation_library = new.designation_library,
              loading_vessel      = new.loading_vessel,
              creation_date       = new.creation_date,
              flowcell_type       = new.flowcell_type,
              lane                = new.lane,
              concentration       = new.concentration,
              is_pool_test        = new.is_pool_test,
              etl_date            = new.etl_date
            WHERE designation_id = new.batch_starting_vessel_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;
          ELSIF V_LATEST_ETL_DATE IS NULL THEN
            -- Note:  designation_id and batch_starting_vessel_id are synonymous
            INSERT INTO flowcell_designation (
              batch_starting_vessel_id, designation_id, fct_id,
              fct_name, fct_type,
              designation_library, loading_vessel, creation_date,
              flowcell_type, lane, concentration,
              is_pool_test, etl_date  )
            VALUES(
              new.batch_starting_vessel_id, new.batch_starting_vessel_id, new.fct_id,
              new.fct_name, new.fct_type,
              new.designation_library, new.loading_vessel, new.creation_date,
              new.flowcell_type, new.lane, new.concentration,
              new.is_pool_test, new.etl_date
            );
            V_INS_COUNT := V_INS_COUNT  + SQL%ROWCOUNT;
            -- ELSE ignore older ETL extract
          END IF;

          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE( TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || ' fct_create.dat (FCT batch vessel ETL) line '
                                || new.line_number || '  ' || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'fct_create (FCT batch vessel ETL)' );

    END MERGE_FCT_BATCH_CRUD;

  PROCEDURE MERGE_FCT_LOAD
  IS
    V_UPD_COUNT PLS_INTEGER;
    V_COUNT NUMBER;
    -- This loop populates flowcell barcode from flowcell loading event
    BEGIN
      V_UPD_COUNT := 0;
      FOR new IN ( SELECT line_number, batch_starting_vessel_id, flowcell_barcode, etl_date
                   FROM im_fct_load
                   WHERE is_delete = 'F' )
      LOOP
        BEGIN
          SELECT COUNT(*)
          INTO V_COUNT
          FROM flowcell_designation
          WHERE designation_id = new.batch_starting_vessel_id;

          IF V_COUNT = 0 THEN
            DBMS_OUTPUT.PUT_LINE( TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_fct_load.dat (FCT load event ETL) line '
                                  || new.line_number || ' - No vessel ID to update: ' || new.batch_starting_vessel_id);
            CONTINUE;
          ELSE
            -- Update only
            UPDATE flowcell_designation
            SET flowcell_barcode = new.flowcell_barcode
            WHERE designation_id   = new.batch_starting_vessel_id;
            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;
          END IF;

          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE( TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_fct_load.dat (FCT load event ETL) line '
                                || new.line_number || '  ' || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, 0, 'fct_load (FCT load event ETL)' );

    END MERGE_FCT_LOAD;


  PROCEDURE MERGE_PRODUCT_ORDER_STATUS
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_PREV_STATUS PRODUCT_ORDER_STATUS.STATUS%TYPE;
    V_PREV_STATUS_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_product_order_status
                  WHERE is_delete = 'F') LOOP
        BEGIN
          -- VERY unlikely unless same file reloaded - status date is ETL date
          UPDATE product_order_status
          SET status = new.status,
            etl_date = new.etl_date
          WHERE product_order_id = new.product_order_id
                AND status_date = new.status_date
                AND etl_date < new.etl_date;

          V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

          -- If same record reloaded and updated, don't bother with additional logic
          IF SQL%ROWCOUNT > 0 THEN
            CONTINUE;
          END IF;

          -- ELSE do an insert only if status has changed since last ETL
          -- (always returns a row, null value if no data found)
          -- TODO: JMS Clean up multiple status rows with no change, keep oldest changed status
          SELECT MAX( status ) KEEP ( DENSE_RANK LAST ORDER BY status_date ASC )
            , MAX( status_date ) KEEP ( DENSE_RANK LAST ORDER BY status_date ASC )
          INTO V_PREV_STATUS, V_PREV_STATUS_DATE
          FROM product_order_status
          WHERE product_order_id = new.product_order_id;

          IF V_PREV_STATUS IS NULL OR ( V_PREV_STATUS <> new.status AND V_PREV_STATUS_DATE < new.status_date ) THEN
            INSERT INTO product_order_status (
              product_order_id,
              status_date,
              status,
              etl_date
            ) VALUES (
              new.product_order_id,
              new.status_date,
              new.status,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
          END IF;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order_status.dat line ' || new.line_number || '  ' ||
              errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'product_order_status' );
    END MERGE_PRODUCT_ORDER_STATUS;


  PROCEDURE MERGE_PDO_SAMPLE_STATUS
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_PREV_STATUS PRODUCT_ORDER_SAMPLE_STATUS.DELIVERY_STATUS%TYPE;
    V_PREV_STATUS_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_product_order_sample_stat
                  WHERE is_delete = 'F') LOOP
        BEGIN
          -- VERY unlikely unless same file reloaded - status date is ETL date
          UPDATE product_order_sample_status
          SET delivery_status = new.delivery_status,
            etl_date = new.etl_date
          WHERE product_order_sample_id = new.product_order_sample_id
                AND status_date = new.status_date
                AND etl_date < new.etl_date;

          V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

          -- If same record reloaded and updated, don't bother with additional logic
          IF SQL%ROWCOUNT > 0 THEN
            CONTINUE;
          END IF;
          -- ELSE do an insert only if status has changed since last ETL
          -- (always returns a row, null value if no data found)
          -- TODO: JMS Clean up multiple status rows with no change, keep oldest changed status
          SELECT MAX( delivery_status ) KEEP ( DENSE_RANK LAST ORDER BY status_date ASC )
            , MAX( status_date ) KEEP ( DENSE_RANK LAST ORDER BY status_date ASC )
          INTO V_PREV_STATUS, V_PREV_STATUS_DATE
          FROM product_order_sample_status
          WHERE product_order_sample_id = new.product_order_sample_id;

          IF V_PREV_STATUS IS NULL OR ( V_PREV_STATUS <> new.delivery_status AND V_PREV_STATUS_DATE < new.status_date ) THEN
            INSERT INTO product_order_sample_status (
              product_order_sample_id,
              status_date,
              delivery_status,
              etl_date
            ) VALUES (
              new.product_order_sample_id,
              new.status_date,
              new.delivery_status,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
          END IF;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order_sample_status.dat line ' || new.line_number ||
              '  ' || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'product_order_sample_status' );
    END MERGE_PDO_SAMPLE_STATUS;


  PROCEDURE MERGE_PDO_SAMPLE_RISK
  IS
    V_UPD_COUNT PLS_INTEGER;
    BEGIN
      V_UPD_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_product_order_sample_risk
                  WHERE is_delete = 'F') LOOP
        BEGIN
          -- Only overwrite risk data if risk ETL date is same or newer than product_order_sample ETL date
          UPDATE product_order_sample
          SET on_risk = new.on_risk,
            risk_types = new.risk_types,
            risk_messages = new.risk_messages,
            risk_etl_date = new.etl_date
          WHERE product_order_sample_id = new.product_order_sample_id
                AND risk_etl_date <= new.etl_date;

          V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order_sample_risk.dat line ' || new.line_number || '  '
              || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, 0, 'product_order_sample(risk)' );
    END MERGE_PDO_SAMPLE_RISK;


  PROCEDURE MERGE_PDO_SAMPLE_BILL
  IS
    V_UPD_COUNT PLS_INTEGER;
    BEGIN
      V_UPD_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_product_order_sample_bill
                  WHERE is_delete = 'F') LOOP
        BEGIN
          -- Only overwrite billing data if billing ETL date is same or newer than product_order_sample ETL date
          UPDATE product_order_sample
          SET is_billed = new.is_billed
            , billing_etl_date = new.etl_date
          WHERE product_order_sample_id = new.product_order_sample_id
                AND nvl(billing_etl_date, '01-JAN-1970') <= new.etl_date;

          V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order_sample_bill.dat line ' || new.line_number || '  '
              || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, 0, 'product_order_sample_bill' );
    END MERGE_PDO_SAMPLE_BILL;


  PROCEDURE MERGE_BILLING_SESSION
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      FOR new IN (SELECT *
                  FROM im_billing_session
                  WHERE is_delete = 'F') LOOP
        BEGIN
          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM billing_session
          WHERE billing_session_id = new.billing_session_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE billing_session
            SET billing_session_id = new.billing_session_id,
              billed_date = new.billed_date,
              billing_session_type = new.billing_session_type,
              etl_date = new.etl_date
            WHERE billing_session_id = new.billing_session_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

          ELSIF V_LATEST_ETL_DATE IS NULL THEN

            INSERT INTO billing_session (
              billing_session_id,
              billed_date,
              billing_session_type,
              etl_date
            ) VALUES (
              new.billing_session_id,
              new.billed_date,
              new.billing_session_type,
              new.etl_date );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
            -- ELSE ignore older ETL extract
          END IF;
          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_billing_session.dat line ' || new.line_number ||
              '  ' || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'billing_session' );
    END MERGE_BILLING_SESSION;


  PROCEDURE MERGE_LEDGER_ENTRY
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
    V_UPD_PDOS_COUNT PLS_INTEGER;
    V_LATEST_ETL_DATE DATE;
    BEGIN
      V_INS_COUNT := 0;
      V_UPD_COUNT := 0;
      V_UPD_PDOS_COUNT := 0;
      -- Loop for cross entity ETL.

      FOR new IN (SELECT *
                  FROM im_ledger_entry
                  WHERE is_delete = 'F') LOOP
        BEGIN
          SELECT MAX(ETL_DATE)
          INTO V_LATEST_ETL_DATE
          FROM ledger_entry
          WHERE ledger_id = new.ledger_id;

          -- Do an update only if this ETL date greater than what's in DB already
          IF new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE ledger_entry
            SET ledger_id = new.ledger_id,
              product_order_sample_id = new.product_order_sample_id,
              quote_id = new.quote_id,
              price_item_id = new.price_item_id,
              price_item_type = new.price_item_type,
              quantity = new.quantity,
              billing_session_id = new.billing_session_id,
              billing_message = new.billing_message,
              work_complete_date = new.work_complete_date,
              etl_date = new.etl_date,
              quote_server_work_item = new.quote_server_work_item
            WHERE ledger_id = new.ledger_id;

            V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;
          ELSIF V_LATEST_ETL_DATE IS NULL THEN
            INSERT INTO ledger_entry (
              ledger_id,
              product_order_sample_id,
              quote_id,
              price_item_id,
              price_item_type,
              quantity,
              billing_session_id,
              billing_message,
              work_complete_date,
              etl_date,
              quote_server_work_item
            ) VALUES (
              new.ledger_id,
              new.product_order_sample_id,
              new.QUOTE_ID,
              new.price_item_id,
              new.price_item_type,
              new.quantity,
              new.billing_session_id,
              new.billing_message,
              new.work_complete_date,
              new.etl_date,
              new.quote_server_work_item );

            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
            -- ELSE ignore older ETL extract
          END IF;

          -- Only overwrite billing data if billing ETL date is same or newer than product_order_sample ETL date
          IF V_LATEST_ETL_DATE IS NULL OR new.etl_date > V_LATEST_ETL_DATE THEN
            UPDATE product_order_sample
            SET ledger_quote_id = new.quote_id
            WHERE product_order_sample_id = new.product_order_sample_id;

            V_UPD_PDOS_COUNT := V_UPD_PDOS_COUNT + SQL%ROWCOUNT;
          END IF;

          EXCEPTION WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_ledger_entry.dat line ' || new.line_number ||
              '  ' || errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'ledger_entry' );
      SHOW_ETL_STATS(  V_UPD_PDOS_COUNT, 0, 'product_order_sample.ledger_quote_id' );
    END MERGE_LEDGER_ENTRY;


  PROCEDURE MERGE_SEQUENCING_SAMPLE_FACT
  IS
    V_INS_COUNT PLS_INTEGER;
    BEGIN
      V_INS_COUNT := 0;

      FOR new IN (SELECT *
                  FROM im_sequencing_sample_fact
                  WHERE is_delete = 'F') LOOP
        BEGIN
          -- No update is possible due to lack of common unique key

          BEGIN
            -- Ensures that there is a join to event_fact on pdo and sample
            SELECT 1 INTO v_tmp
            FROM event_fact
            WHERE product_order_id = new.product_order_id
                  AND sample_name = new.sample_name
                  AND ROWNUM = 1;

            EXCEPTION WHEN NO_DATA_FOUND
            THEN RAISE_APPLICATION_ERROR( -20101, 'Sequencing Fact sample and product order not found in Event_Fact table' );
          END;

          BEGIN
            -- Reports an invalid batch_name (NULL, 'NONE', or 'MULTIPLE')
            SELECT 1 INTO v_tmp FROM DUAL
            WHERE NVL(new.batch_name, 'NONE') NOT IN ('NONE', 'MULTIPLE');

            EXCEPTION WHEN NO_DATA_FOUND
            THEN RAISE_APPLICATION_ERROR( -20102, 'Sequencing Fact has invalid lab batch name: ' || NVL(new.batch_name, 'NONE') );
          END;

          INSERT INTO sequencing_sample_fact (
            sequencing_sample_fact_id,
            flowcell_barcode,
            lane,
            molecular_indexing_scheme,
            sequencing_run_id,
            product_order_id,
            sample_name,
            research_project_id,
            loaded_library_barcode,
            loaded_library_create_date,
            batch_name,
            etl_date
          ) VALUES (
            sequencing_sample_id_seq.nextval,
            new.flowcell_barcode,
            new.lane,
            new.molecular_indexing_scheme,
            new.sequencing_run_id,
            new.product_order_id,
            new.sample_name,
            new.research_project_id,
            new.loaded_library_barcode,
            new.loaded_library_create_date,
            new.batch_name,
            new.etl_date );

          V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
          EXCEPTION
          WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_sequencing_sample_fact.dat line ' || new.line_number || '  ' ||
              errmsg);
          CONTINUE;
        END;
      END LOOP;
      SHOW_ETL_STATS(  0, V_INS_COUNT, 'sequencing_sample_fact' );
    END MERGE_SEQUENCING_SAMPLE_FACT;


  /* **********************
   * Removes any deleted data from warehouse tables
   * Updates rows when they exist in the target table, inserts rows when they do not exist.
   * ********************** */
  PROCEDURE DO_ETL
  AS
    BEGIN

      V_ETL_MOD_TIMESTAMP := SYSDATE;

      -- Remove any deleted records from data warehouse
      DO_DELETES();

      -- Merge imported data (order is important to support parent-child dependencies)
      MERGE_RESEARCH_PROJECT();
      MERGE_RESEARCH_PROJECT_STATUS();
      MERGE_PRICE_ITEM();
      MERGE_PRODUCT();
      MERGE_LAB_VESSEL();
      MERGE_ABANDON_VESSEL();
      MERGE_WORKFLOW();
      MERGE_WORKFLOW_PROCESS();
      MERGE_SEQUENCING_RUN();
      MERGE_REGULATORY_INFO();
      MERGE_PRODUCT_ORDER();
      MERGE_PRODUCT_ORDER_STATUS();
      MERGE_LAB_METRIC();
      MERGE_PRODUCT_ORDER_ADD_ON();
      MERGE_RESEARCH_PROJECT_PERSON();
      MERGE_RESEARCH_PROJECT_FUNDING();
      MERGE_RESEARCH_PROJECT_COHORT();
      MERGE_RESEARCH_PROJECT_IRB();
      MERGE_PRODUCT_ORDER_SAMPLE();
      MERGE_PDO_SAMPLE_STATUS();
      MERGE_EVENT_FACT();
      MERGE_ANCESTRY();
      MERGE_LIBRARY_SAMPLE();
      MERGE_ARRAY_PROCESS_FLOW();
      MERGE_FCT_BATCH_CRUD();
      MERGE_FCT_LOAD();

      -- Level 3 (depends on level 2 tables)
      MERGE_PDO_SAMPLE_RISK();
      MERGE_LEDGER_ENTRY();
      MERGE_PDO_SAMPLE_BILL();
      MERGE_BILLING_SESSION();
      MERGE_SEQUENCING_SAMPLE_FACT();

      COMMIT;

    END DO_ETL;

END MERGE_ETL_IMPORT;
/

SHOW ERRORS

