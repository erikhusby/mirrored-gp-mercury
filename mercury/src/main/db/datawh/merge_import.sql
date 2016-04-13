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
  BEGIN

    -- For event fact table, a re-export of audited entity ids should replace existing ones.
    DELETE FROM LIBRARY_ANCESTRY
     WHERE CHILD_EVENT_ID IN (
       SELECT DISTINCT LAB_EVENT_ID
         FROM IM_EVENT_FACT );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' library_ancestry child rows' );

    DELETE FROM event_fact
    WHERE lab_event_id IN (SELECT
                             DISTINCT lab_event_id
                           FROM im_event_fact);
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' event_fact rows' );

    -- Wipe out any ancestry links in LIBRARY_LCSET_SAMPLE_BASE - all to be re-imported
    DELETE FROM LIBRARY_LCSET_SAMPLE_BASE
     WHERE ( library_id, library_type )
           IN (SELECT ancestor_library_id, ancestor_library_type FROM im_library_ancestry
               UNION
               SELECT child_library_id, child_library_type FROM im_library_ancestry );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' library_lcset_sample_base rows' );

    DELETE FROM product_order_sample
    WHERE product_order_sample_id IN (
      SELECT
        product_order_sample_id
      FROM im_product_order_sample
      WHERE is_delete = 'T'
    );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' product_order_sample rows' );

    DELETE FROM research_project_status
    WHERE research_project_id IN (
      SELECT
        research_project_id
      FROM im_research_project_status
      WHERE is_delete = 'T'
    );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' research_project_status rows' );

    DELETE FROM research_project_person
    WHERE research_project_person_id IN (
      SELECT
        research_project_person_id
      FROM im_research_project_person
      WHERE is_delete = 'T'
    );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' research_project_person rows' );

    DELETE FROM research_project_funding
    WHERE research_project_funding_id IN (
      SELECT
        research_project_funding_id
      FROM im_research_project_funding
      WHERE is_delete = 'T'
    );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' research_project_funding rows' );

    DELETE FROM research_project_cohort
    WHERE research_project_cohort_id IN (
      SELECT
        research_project_cohort_id
      FROM im_research_project_cohort
      WHERE is_delete = 'T'
    );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' research_project_cohort rows' );

    DELETE FROM research_project_irb
    WHERE research_project_irb_id IN (
      SELECT
        research_project_irb_id
      FROM im_research_project_irb
      WHERE is_delete = 'T'
    );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' research_project_irb rows' );

    DELETE FROM product_order_add_on
    WHERE product_order_add_on_id IN (
      SELECT
        product_order_add_on_id
      FROM im_product_order_add_on
      WHERE is_delete = 'T'
    );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' product_order_add_on rows' );

    DELETE FROM price_item
    WHERE price_item_id IN (
      SELECT
        price_item_id
      FROM im_price_item
      WHERE is_delete = 'T'
    );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' price_item rows' );

    -- Flush PDO regulatory info for all changed/deleted product orders
    DELETE FROM pdo_regulatory_infos
    WHERE product_order IN (
      SELECT
        product_order_id
      FROM im_product_order
    );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' pdo_regulatory_infos rows' );

    -- Doubtful regulatory info will ever be deleted, but handle it
    DELETE FROM regulatory_info
    WHERE regulatory_info_id IN (
      SELECT
        regulatory_info_id
      FROM im_regulatory_info
      WHERE is_delete = 'T'
    );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' regulatory_info rows' );

    DELETE FROM product_order
    WHERE product_order_id IN (
      SELECT
        product_order_id
      FROM im_product_order
      WHERE is_delete = 'T'
    );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' product_order rows' );

    DELETE FROM lab_vessel
    WHERE lab_vessel_id IN (
      SELECT
        lab_vessel_id
      FROM im_lab_vessel
      WHERE is_delete = 'T'
    );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' lab_vessel rows' );

    DELETE FROM lab_metric
    WHERE lab_metric_id IN (SELECT lab_metric_id FROM im_lab_metric WHERE is_delete = 'T');
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' lab_metric rows' );

    DELETE FROM product
    WHERE product_id IN (
      SELECT
        product_id
      FROM im_product
      WHERE is_delete = 'T'
    );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' product rows' );

    DELETE FROM research_project
    WHERE research_project_id IN (
      SELECT
        research_project_id
      FROM im_research_project
      WHERE is_delete = 'T'
    );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' research_project rows' );

    DELETE FROM sequencing_run
    WHERE sequencing_run_id IN (
      SELECT
        sequencing_run_id
      FROM im_sequencing_run
      WHERE is_delete = 'T'
    );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' sequencing_run rows' );

    DELETE FROM ledger_entry
    WHERE ledger_id IN (
      SELECT
        ledger_id
      FROM im_ledger_entry
      WHERE is_delete = 'T'
    );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' ledger_entry rows' );

    DELETE FROM billing_session
    WHERE billing_session_id IN (
      SELECT
        billing_session_id
      FROM im_billing_session
      WHERE is_delete = 'T'
    );
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' billing_session rows' );

    -- For this fact table, a re-export of audited entities should replace
    -- existing ones.  Sequencing will reuse flowcell barcode when redoing a
    -- run, so replace based on flowcell barcode too, provided the imported
    -- sequencing run is later than the exiting one.
    DELETE FROM sequencing_sample_fact
    WHERE sequencing_sample_fact_id IN
          (SELECT ssf.sequencing_sample_fact_id
           FROM sequencing_sample_fact ssf, im_sequencing_sample_fact issf
           WHERE ssf.sequencing_run_id = issf.sequencing_run_id
                 OR (ssf.flowcell_barcode = issf.flowcell_barcode
                     AND ssf.sequencing_run_id < issf.sequencing_run_id));
    DBMS_OUTPUT.PUT_LINE( 'Deleted ' || SQL%ROWCOUNT || ' sequencing_sample_fact rows' );

    COMMIT;

  END DO_DELETES;

  PROCEDURE MERGE_RESEARCH_PROJECT
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_research_project
                 WHERE is_delete = 'F')
    LOOP
      BEGIN
        UPDATE research_project
        SET
          current_status = new.current_status,
          created_date = new.created_date,
          title = new.title,
          irb_not_engaged = new.irb_not_engaged,
          jira_ticket_key = new.jira_ticket_key,
          parent_research_project_id = new.parent_research_project_id,
          root_research_project_id = new.root_research_project_id,
          etl_date = new.etl_date
        WHERE research_project_id = new.research_project_id;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

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
        )
          SELECT
            new.research_project_id,
            new.current_status,
            new.created_date,
            new.title,
            new.irb_not_engaged,
            new.jira_ticket_key,
            new.parent_research_project_id,
            new.root_research_project_id,
            new.etl_date
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM research_project
              WHERE research_project_id = new.research_project_id
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
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
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_price_item
                 WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE price_item
        SET
          platform = new.platform,
          category = new.category,
          price_item_name = new.price_item_name,
          quote_server_id = new.quote_server_id,
          price = new.price,
          units = new.units,
          etl_date = new.etl_date
        WHERE price_item_id = new.price_item_id;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

        INSERT INTO price_item (
          price_item_id,
          platform,
          category,
          price_item_name,
          quote_server_id,
          price,
          units,
          etl_date
        )
          SELECT
            new.price_item_id,
            new.platform,
            new.category,
            new.price_item_name,
            new.quote_server_id,
            new.price,
            new.units,
            new.etl_date
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM price_item
              WHERE price_item_id = new.price_item_id
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
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
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT
                  *
                FROM im_product
                WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE product
        SET
          product_name  = new.product_name,
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
          etl_date = new.etl_date
        WHERE product_id = new.product_id;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

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
          etl_date
        )
          SELECT
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
            new.etl_date
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM product
              WHERE product_id = new.product_id
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
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
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_lab_vessel
                 WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE lab_vessel
        SET
          lab_vessel_id = new.lab_vessel_id,
          label = new.label,
          lab_vessel_type = new.lab_vessel_type,
          etl_date = new.etl_date
        WHERE lab_vessel_id = new.lab_vessel_id;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

        INSERT INTO lab_vessel (
          lab_vessel_id,
          label,
          lab_vessel_type,
          etl_date
        )
          SELECT
            new.lab_vessel_id,
            new.label,
            new.lab_vessel_type,
            new.etl_date
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM lab_vessel
              WHERE lab_vessel_id = new.lab_vessel_id
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
      EXCEPTION WHEN OTHERS THEN
        errmsg := SQLERRM;
        DBMS_OUTPUT.PUT_LINE(
            TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_lab_vessel.dat line ' || new.line_number || '  ' || errmsg);
        CONTINUE;
      END;
    END LOOP;
    SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'lab_vessel' );
  END MERGE_LAB_VESSEL;

  PROCEDURE MERGE_LAB_METRIC
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;

    FOR new IN (SELECT * FROM im_lab_metric WHERE is_delete = 'F') LOOP
      BEGIN

        -- RPT-3131 - Delete any older metrics for same vessel
        DELETE FROM lab_metric
        WHERE vessel_barcode =  new.vessel_barcode
              AND quant_type     =  new.quant_type
              AND run_date       < new.run_date;

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

        IF SQL%ROWCOUNT = 0 THEN
          INSERT INTO lab_metric (
                lab_metric_id,
                quant_type, quant_units, quant_value,
                run_name, run_date,
                lab_vessel_id, vessel_barcode, rack_position,
                decision, decision_date, decider,
                override_reason, etl_date )
            SELECT new.lab_metric_id,
                   new.quant_type, new.quant_units, new.quant_value,
                   new.run_name, new.run_date,
                   new.lab_vessel_id, new.vessel_barcode, new.rack_position,
                   new.decision, new.decision_date, new.decider,
                   new.override_reason, new.etl_date
              FROM dual
             WHERE NOT EXISTS (
                SELECT 'Y'
                  FROM lab_metric
                 WHERE vessel_barcode =  new.vessel_barcode
                   AND quant_type     =  new.quant_type
                   AND run_date       > new.run_date );

          V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;

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
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_workflow
                 WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE workflow
        SET
          workflow_id = new.workflow_id,
          workflow_name = new.workflow_name,
          workflow_version = new.workflow_version,
          etl_date = new.etl_date
        WHERE workflow_id = new.workflow_id;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

        INSERT INTO workflow (
          workflow_id,
          workflow_name,
          workflow_version,
          etl_date
        )
          SELECT
            new.workflow_id,
            new.workflow_name,
            new.workflow_version,
            new.etl_date
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM workflow
              WHERE workflow_id = new.workflow_id
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
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
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_workflow_process
                 WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE workflow_process
        SET
          process_id = new.process_id,
          process_name = new.process_name,
          process_version = new.process_version,
          step_name = new.step_name,
          event_name = new.event_name,
          etl_date = new.etl_date
        WHERE process_id = new.process_id;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

        INSERT INTO workflow_process (
          process_id,
          process_name,
          process_version,
          step_name,
          event_name,
          etl_date
        )
          SELECT
            new.process_id,
            new.process_name,
            new.process_version,
            new.step_name,
            new.event_name,
            new.etl_date
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM workflow_process
              WHERE process_id = new.process_id
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
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
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_sequencing_run
                 WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE sequencing_run
        SET
          run_name = new.run_name,
          barcode = new.barcode,
          registration_date = new.registration_date,
          instrument = new.instrument,
          setup_read_structure = new.setup_read_structure,
          actual_read_structure = new.actual_read_structure,
          etl_date = new.etl_date
        WHERE sequencing_run_id = new.sequencing_run_id;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

        INSERT INTO sequencing_run (
          sequencing_run_id,
          run_name,
          barcode,
          registration_date,
          instrument,
          setup_read_structure,
          actual_read_structure,
          etl_date
        )
          SELECT
            new.sequencing_run_id,
            new.run_name,
            new.barcode,
            new.registration_date,
            new.instrument,
            new.setup_read_structure,
            new.actual_read_structure,
            new.etl_date
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM sequencing_run
              WHERE sequencing_run_id = new.sequencing_run_id
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
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
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    -- Regulatory info data
    FOR new IN ( SELECT *
                 FROM im_regulatory_info
                 WHERE is_delete = 'F' ) LOOP
      BEGIN
        UPDATE regulatory_info
        SET regulatory_info_id = new.regulatory_info_id,
          identifier = new.identifier,
          type       = new.type,
          name       = new.name,
          etl_date   = new.etl_date
        WHERE regulatory_info_id = new.regulatory_info_id;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

        IF SQL%ROWCOUNT = 0 THEN
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
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_product_order
                WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE product_order
        SET
          research_project_id = new.research_project_id,
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
          etl_date = new.etl_date
        WHERE product_order_id = new.product_order_id;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

        IF SQL%ROWCOUNT = 0 THEN
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
            new.etl_date );

          V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
        END IF ;
      EXCEPTION WHEN OTHERS THEN
        errmsg := SQLERRM;
        DBMS_OUTPUT.PUT_LINE(
            TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order.dat line ' || new.line_number || '  ' || errmsg);
        CONTINUE;
      END;
    END LOOP;

    SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'product_order' );

    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    -- Many to many mapping table pdo_regulatory_infos
    -- List of regulatory_info_id's associated with each product_order_id in a list
    -- See function PKS_FROM_JAVA_ARRAYSTOSTRING
    FOR new IN ( SELECT line_number, product_order_id, reg_info_ids, etl_date
                   FROM im_product_order
                  WHERE is_delete = 'F' )
    LOOP
      BEGIN
        V_PK_ARR := PKS_FROM_JAVA_ARRAYSTOSTRING( new.reg_info_ids );
        IF V_PK_ARR.COUNT > 0 THEN
          FOR V_INDEX IN V_PK_ARR.FIRST .. V_PK_ARR.LAST
          LOOP
            INSERT INTO pdo_regulatory_infos (
              product_order,
              regulatory_infos,
              etl_date
            ) VALUES (
              new.product_order_id,
              V_PK_ARR(V_INDEX),
              new.etl_date );
            V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
          END LOOP;
        END IF;
      EXCEPTION WHEN OTHERS THEN
        errmsg := SQLERRM;
        DBMS_OUTPUT.PUT_LINE(
            TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order.dat line, pdo_regulatory_infos data '
            || new.line_number || '  ' || errmsg);
        CONTINUE;
      END;
    END LOOP;
    SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'pdo_regulatory_infos' );
  END MERGE_PRODUCT_ORDER;

  PROCEDURE MERGE_PRODUCT_ORDER_ADD_ON
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_product_order_add_on
                 WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE product_order_add_on
        SET
          product_order_id = new.product_order_id,
          product_id = new.product_id,
          etl_date = new.etl_date
        WHERE product_order_add_on_id = new.product_order_add_on_id;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

        INSERT INTO product_order_add_on (
          product_order_add_on_id,
          product_order_id,
          product_id,
          etl_date
        )
          SELECT
            new.product_order_add_on_id,
            new.product_order_id,
            new.product_id,
            new.etl_date
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM product_order_add_on
              WHERE product_order_add_on_id = new.product_order_add_on_id
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
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
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_research_project_status
                 WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE research_project_status
        SET
          status_date = new.status_date,
          status = new.status,
          etl_date = new.etl_date
        WHERE research_project_id = new.research_project_id
              AND status_date = new.status_date;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

        INSERT INTO research_project_status (
          research_project_id,
          status_date,
          status,
          etl_date
        )
          SELECT
            new.research_project_id,
            new.status_date,
            new.status,
            new.etl_date
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM research_project_status
              WHERE research_project_id = new.research_project_id
                    AND status_date = new.status_date
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
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
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_research_project_person
                 WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE research_project_person
        SET
          research_project_id = new.research_project_id,
          project_role = new.project_role,
          person_id = new.person_id,
          first_name = new.first_name,
          last_name = new.last_name,
          username = new.username,
          etl_date = new.etl_date
        WHERE research_project_person_id = new.research_project_person_id;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

        INSERT INTO research_project_person (
          research_project_person_id,
          research_project_id,
          project_role,
          person_id,
          first_name,
          last_name,
          username,
          etl_date
        )
          SELECT
            new.research_project_person_id,
            new.research_project_id,
            new.project_role,
            new.person_id,
            new.first_name,
            new.last_name,
            new.username,
            new.etl_date
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM research_project_person
              WHERE research_project_person_id = new.research_project_person_id
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
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
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_research_project_funding
                 WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE research_project_funding
        SET
          funding_id = new.funding_id,
          etl_date = new.etl_date
        WHERE research_project_funding_id = new.research_project_funding_id;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

        INSERT INTO research_project_funding (
          research_project_id,
          research_project_funding_id,
          funding_id,
          etl_date
        )
          SELECT
            new.research_project_id,
            new.research_project_funding_id,
            new.funding_id,
            new.etl_date
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM research_project_funding
              WHERE research_project_funding_id = new.research_project_funding_id
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
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
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                FROM im_research_project_cohort
                WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE research_project_cohort
        SET
          etl_date = new.etl_date
        WHERE research_project_cohort_id = new.research_project_cohort_id;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

        INSERT INTO research_project_cohort (
          research_project_id,
          research_project_cohort_id,
          etl_date
        )
          SELECT
            new.research_project_id,
            new.research_project_cohort_id,
            new.etl_date
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM research_project_cohort
              WHERE research_project_cohort_id = new.research_project_cohort_id
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
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
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_research_project_irb
                 WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE research_project_irb
        SET
          research_project_id = new.research_project_id,
          research_project_irb = new.research_project_irb,
          research_project_irb_type = new.research_project_irb_type,
          etl_date = new.etl_date
        WHERE research_project_irb_id = new.research_project_irb_id;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

        INSERT INTO research_project_irb (
          research_project_irb_id,
          research_project_id,
          research_project_irb,
          research_project_irb_type,
          etl_date
        )
          SELECT
            new.research_project_irb_id,
            new.research_project_id,
            new.research_project_irb,
            new.research_project_irb_type,
            new.etl_date
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM research_project_irb
              WHERE research_project_irb_id = new.research_project_irb_id
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
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
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_product_order_sample
                 WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE product_order_sample
        SET
          product_order_id = new.product_order_id,
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
          etl_date
        )
          SELECT
            new.product_order_sample_id,
            new.product_order_id,
            new.sample_name,
            new.delivery_status,
            new.sample_position,
            new.PARTICIPANT_ID,
            new.SAMPLE_TYPE,
            new.SAMPLE_RECEIPT,
            new.ORIGINAL_SAMPLE_TYPE,
            new.etl_date
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM product_order_sample
              WHERE product_order_sample_id = new.product_order_sample_id
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
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
    V_UPD_COUNT PLS_INTEGER;
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    -- Only sets PK when it is null, so we can do an idempotent repeatable merge.
    UPDATE im_event_fact
    SET event_fact_id = event_fact_id_seq.nextval
    WHERE event_fact_id IS NULL;

    FOR new IN (SELECT *
                  FROM im_event_fact
                 WHERE is_delete = 'F') LOOP
      BEGIN
        -- No update is possible due to lack of common unique key
        INSERT INTO event_fact (
          event_fact_id,
          lab_event_id,
          workflow_id,
          process_id,
          lab_event_type,
          product_order_id,
          sample_name,
          lcset_sample_name,
          batch_name,
          station_name,
          lab_vessel_id,
          position,
          event_date,
          etl_date,
          program_name,
          molecular_indexing_scheme
        )
          SELECT
            new.event_fact_id,
            new.lab_event_id,
            new.workflow_id,
            new.process_id,
            new.lab_event_type,
            new.product_order_id,
            new.sample_name,
            new.lcset_sample_name,
            new.batch_name,
            new.station_name,
            new.lab_vessel_id,
            new.position,
            new.event_date,
            new.etl_date,
            new.program_name,
            new.molecular_indexing_scheme
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM event_fact
              WHERE event_fact_id = new.event_fact_id
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;

      EXCEPTION
        WHEN OTHERS THEN
          errmsg := SQLERRM;
          DBMS_OUTPUT.PUT_LINE(
              TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_event_fact.dat line ' || new.line_number || '  ' || errmsg);
          CONTINUE;
      END;
    END LOOP;
    SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'event_fact' );
  END MERGE_EVENT_FACT;


  PROCEDURE MERGE_ANCESTRY
  IS
    V_ANCEST_INS_COUNT PLS_INTEGER;
    -- Nothing ever updated
    V_UPD_COUNT CONSTANT PLS_INTEGER := 0;
  BEGIN
    V_ANCEST_INS_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_library_ancestry
                 WHERE is_delete = 'F') LOOP
      BEGIN
        INSERT INTO library_ancestry (
          ancestor_event_id,
          ancestor_library_id,
          ancestor_library_type,
          ancestor_library_creation,
          child_event_id,
          child_library_id,
          child_library_type,
          child_library_creation,
          etl_date
        ) VALUES (
          new.ancestor_event_id,
          new.ancestor_library_id,
          new.ancestor_library_type,
          new.ancestor_library_creation,
          new.child_event_id,
          new.child_library_id,
          new.child_library_type,
          new.child_library_creation,
          new.etl_date
        );
        V_ANCEST_INS_COUNT := V_ANCEST_INS_COUNT + SQL%ROWCOUNT;

      EXCEPTION WHEN OTHERS THEN
        errmsg := SQLERRM;
        DBMS_OUTPUT.PUT_LINE(
            TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_library_ancestry.dat line ' || new.line_number || '  ' || errmsg);
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
    BEGIN
      V_LIBRARY_INS_COUNT := 0;
      FOR new IN (SELECT library_id, event_id
                       , library_type, library_creation
                       , MIN(line_number) as line_number, etl_date
                    FROM (
                      SELECT child_library_id AS library_id,
                             -- Is there more than 1 event in this file for a vessel?
                        MAX(child_event_id) KEEP ( DENSE_RANK LAST ORDER BY child_library_creation ) AS event_id,
                             child_library_type as library_type,
                             MAX(child_library_creation) as library_creation,
                        MIN(line_number) as line_number,
                        MAX(etl_date) AS etl_date
                        FROM im_library_ancestry
                       WHERE is_delete = 'F'
                      GROUP BY child_library_id, child_library_type
                      UNION
                      SELECT ancestor_library_id,
                             MAX(ancestor_event_id) KEEP ( DENSE_RANK LAST ORDER BY ancestor_library_creation ),
                             ancestor_library_type,
                             MAX(ancestor_library_creation),
                             MIN(line_number) as line_number,
                             MAX(etl_date) AS etl_date
                        FROM im_library_ancestry
                       WHERE is_delete = 'F'
                      GROUP BY ancestor_library_id, ancestor_library_type
                  )
                    GROUP BY library_id, event_id
                           , library_type, library_creation, etl_date
          ) LOOP
        BEGIN
          -- Rows for libraries - we've already deleted any rows related to libraries
          INSERT INTO LIBRARY_LCSET_SAMPLE_BASE (
            LIBRARY_LABEL, LIBRARY_ID,
            LIBRARY_TYPE, LIBRARY_CREATION_DATE, LIBRARY_EVENT_ID )
          VALUES(
            ( SELECT label FROM lab_vessel where lab_vessel_id = new.library_id ),
            new.library_id,
            new.library_type, new.library_creation, new.event_id
          );

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


  PROCEDURE MERGE_PRODUCT_ORDER_STATUS
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_product_order_status
                 WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE product_order_status
        SET
          status = new.status,
          etl_date = new.etl_date
        WHERE product_order_id = new.product_order_id
              AND status_date = new.status_date;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

        INSERT INTO product_order_status (
          product_order_id,
          status_date,
          status,
          etl_date
        )
          SELECT
            new.product_order_id,
            new.status_date,
            new.status,
            new.etl_date
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM product_order_status
              WHERE product_order_id = new.product_order_id
                    AND status_date = new.status_date
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
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
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_product_order_sample_stat
                 WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE product_order_sample_status
        SET
          delivery_status = new.delivery_status,
          etl_date = new.etl_date
        WHERE product_order_sample_id = new.product_order_sample_id
              AND status_date = new.status_date;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

        INSERT INTO product_order_sample_status (
          product_order_sample_id,
          status_date,
          delivery_status,
          etl_date
        )
          SELECT
            new.product_order_sample_id,
            new.status_date,
            new.delivery_status,
            new.etl_date
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM product_order_sample_status
              WHERE product_order_sample_id = new.product_order_sample_id
                    AND status_date = new.status_date
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
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
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_product_order_sample_risk
                 WHERE is_delete = 'F') LOOP
      BEGIN

        UPDATE product_order_sample
        SET
          on_risk = new.on_risk,
          risk_types = new.risk_types,
          risk_messages = new.risk_messages
        WHERE product_order_sample_id = new.product_order_sample_id;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

      EXCEPTION WHEN OTHERS THEN
        errmsg := SQLERRM;
        DBMS_OUTPUT.PUT_LINE(
            TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order_sample_risk.dat line ' || new.line_number || '  '
            || errmsg);
        CONTINUE;
      END;
    END LOOP;
    SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'product_order_sample' );
  END MERGE_PDO_SAMPLE_RISK;


  PROCEDURE MERGE_PDO_SAMPLE_BILL
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_product_order_sample_bill
                 WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE product_order_sample
        SET
          is_billed = new.is_billed
        WHERE product_order_sample_id = new.product_order_sample_id;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;
      EXCEPTION WHEN OTHERS THEN
        errmsg := SQLERRM;
        DBMS_OUTPUT.PUT_LINE(
            TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order_sample_bill.dat line ' || new.line_number || '  '
            || errmsg);
        CONTINUE;
      END;
    END LOOP;
    SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'product_order_sample_bill' );
  END MERGE_PDO_SAMPLE_BILL;


  PROCEDURE MERGE_BILLING_SESSION
  IS
    V_INS_COUNT PLS_INTEGER;
    V_UPD_COUNT PLS_INTEGER;
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    FOR new IN (SELECT *
                  FROM im_billing_session
                 WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE billing_session
        SET
          billing_session_id = new.billing_session_id,
          billed_date = new.billed_date,
          billing_session_type = new.billing_session_type,
          etl_date = new.etl_date
        WHERE billing_session_id = new.billing_session_id;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

        INSERT INTO billing_session (
          billing_session_id,
          billed_date,
          billing_session_type,
          etl_date
        )
          SELECT
            new.billing_session_id,
            new.billed_date,
            new.billing_session_type,
            new.etl_date
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM billing_session
              WHERE billing_session_id = new.billing_session_id
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
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
  BEGIN
    V_INS_COUNT := 0;
    V_UPD_COUNT := 0;
    -- Loop for cross entity ETL.
    FOR new IN (SELECT *
                  FROM im_ledger_entry
                 WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE product_order_sample
        SET
          ledger_quote_id = new.quote_id
        WHERE product_order_sample_id = new.product_order_sample_id;

        V_UPD_COUNT := V_UPD_COUNT + SQL%ROWCOUNT;

      EXCEPTION WHEN OTHERS THEN
        errmsg := SQLERRM;
        DBMS_OUTPUT.PUT_LINE(
            TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_ledger_entry.dat line ' || new.line_number || '  ' || errmsg);
        CONTINUE;
      END;
    END LOOP;

    SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'product_order_sample.ledger_quote_id' );
    V_UPD_COUNT := 0;

    FOR new IN (SELECT *
                  FROM im_ledger_entry
                 WHERE is_delete = 'F') LOOP
      BEGIN
        UPDATE ledger_entry
        SET
          ledger_id = new.ledger_id,
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
        )
          SELECT
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
            new.quote_server_work_item
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM ledger_entry
              WHERE ledger_id = new.ledger_id
          );

        V_INS_COUNT := V_INS_COUNT + SQL%ROWCOUNT;
      EXCEPTION WHEN OTHERS THEN
        errmsg := SQLERRM;
        DBMS_OUTPUT.PUT_LINE(
            TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_ledger_entry.dat line ' || new.line_number ||
            '  ' || errmsg);
        CONTINUE;
      END;
    END LOOP;
    SHOW_ETL_STATS(  V_UPD_COUNT, V_INS_COUNT, 'ledger_entry' );
  END MERGE_LEDGER_ENTRY;


  PROCEDURE MERGE_SEQUENCING_SAMPLE_FACT
  IS
    V_INS_COUNT PLS_INTEGER;
  BEGIN
    V_INS_COUNT := 0;
    -- Only sets PK when it is null, so we can do an idempotent repeatable merge.
    UPDATE im_sequencing_sample_fact
    SET sequencing_sample_fact_id = sequencing_sample_id_seq.nextval
    WHERE sequencing_sample_fact_id IS NULL;

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
        )
          SELECT
            new.sequencing_sample_fact_id,
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
            new.etl_date
          FROM DUAL
          WHERE NOT EXISTS(
              SELECT
                1
              FROM sequencing_sample_fact
              WHERE sequencing_sample_fact_id = new.sequencing_sample_fact_id
          );

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
    -- Remove any deleted records from data warehouse
    DO_DELETES();

    -- Merge imported data (order is important to support parent-child dependencies)
    MERGE_RESEARCH_PROJECT();
    MERGE_RESEARCH_PROJECT_STATUS();
    MERGE_PRICE_ITEM();
    MERGE_PRODUCT();
    MERGE_LAB_VESSEL();
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

