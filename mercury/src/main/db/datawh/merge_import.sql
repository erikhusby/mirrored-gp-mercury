/*
 Does insert/update/delete to all reporting tables, using data from import tables.
*/
CREATE OR REPLACE PROCEDURE merge_import
IS

  CURSOR im_po_cur IS SELECT
                        *
                      FROM im_product_order
                      WHERE is_delete = 'F';
  CURSOR im_po_sample_cur IS SELECT
                               *
                             FROM im_product_order_sample
                             WHERE is_delete = 'F';
  CURSOR im_po_status_cur IS SELECT
                               *
                             FROM im_product_order_status
                             WHERE is_delete = 'F';
  CURSOR im_po_sample_stat_cur IS SELECT
                                    *
                                  FROM im_product_order_sample_stat
                                  WHERE is_delete = 'F';
  CURSOR im_price_item_cur IS SELECT
                                *
                              FROM im_price_item
                              WHERE is_delete = 'F';
  CURSOR im_po_add_on_cur IS SELECT
                               *
                             FROM im_product_order_add_on
                             WHERE is_delete = 'F';
  CURSOR im_product_cur IS SELECT
                             *
                           FROM im_product
                           WHERE is_delete = 'F';
  CURSOR im_rp_cohort_cur IS SELECT
                               *
                             FROM im_research_project_cohort
                             WHERE is_delete = 'F';
  CURSOR im_rp_cur IS SELECT
                        *
                      FROM im_research_project
                      WHERE is_delete = 'F';
  CURSOR im_rp_funding_cur IS SELECT
                                *
                              FROM im_research_project_funding
                              WHERE is_delete = 'F';
  CURSOR im_rp_irb_cur IS SELECT
                            *
                          FROM im_research_project_irb
                          WHERE is_delete = 'F';
  CURSOR im_rp_person_cur IS SELECT
                               *
                             FROM im_research_project_person
                             WHERE is_delete = 'F';
  CURSOR im_rp_status_cur IS SELECT
                               *
                             FROM im_research_project_status
                             WHERE is_delete = 'F';
  CURSOR im_lab_vessel_cur IS SELECT
                                *
                              FROM im_lab_vessel
                              WHERE is_delete = 'F';
  CURSOR im_workflow_cur IS SELECT
                              *
                            FROM im_workflow
                            WHERE is_delete = 'F';
  CURSOR im_workflow_process_cur IS SELECT
                                      *
                                    FROM im_workflow_process
                                    WHERE is_delete = 'F';
  CURSOR im_event_fact_cur IS SELECT
                                *
                              FROM im_event_fact
                              WHERE is_delete = 'F';
  CURSOR im_po_sample_risk_cur IS SELECT
                                    *
                                  FROM im_product_order_sample_risk
                                  WHERE is_delete = 'F';
  CURSOR im_po_sample_bill_cur IS SELECT
                                    *
                                  FROM im_product_order_sample_bill
                                  WHERE is_delete = 'F';
  CURSOR im_ledger_entry_cur IS SELECT
                                  *
                                FROM im_ledger_entry
                                WHERE is_delete = 'F';
  CURSOR im_sequencing_run_cur IS SELECT
                                    *
                                  FROM im_sequencing_run
                                  WHERE is_delete = 'F';
  CURSOR im_seq_sample_fact_cur IS SELECT
                                     *
                                   FROM im_sequencing_sample_fact
                                   WHERE is_delete = 'F';
  CURSOR im_billing_session_cur IS SELECT
                                     *
                                   FROM im_billing_session
                                   WHERE is_delete = 'F';

  CURSOR im_lab_metric_cur IS SELECT * FROM im_lab_metric WHERE is_delete = 'F';

  errmsg        VARCHAR2(255);
  PDO_SAMPLE_NOT_IN_EVENT_FACT EXCEPTION;
  INVALID_LAB_BATCH EXCEPTION;
  v_tmp  NUMBER;

  BEGIN

---------------------------------------------------------------------------
-- Deletes rows in the reporting tables when the import has is_delete = 'T'.
-- Does the most dependent (FK dependency) tables first.
---------------------------------------------------------------------------

    -- For this fact table, a re-export of audited entity ids should replace existing ones.
    DELETE FROM event_fact
    WHERE lab_event_id IN (SELECT
                             DISTINCT lab_event_id
                           FROM im_event_fact);

    DELETE FROM product_order_sample
    WHERE product_order_sample_id IN (
      SELECT
        product_order_sample_id
      FROM im_product_order_sample
      WHERE is_delete = 'T'
    );

    DELETE FROM research_project_status
    WHERE research_project_id IN (
      SELECT
        research_project_id
      FROM im_research_project_status
      WHERE is_delete = 'T'
    );

    DELETE FROM research_project_person
    WHERE research_project_person_id IN (
      SELECT
        research_project_person_id
      FROM im_research_project_person
      WHERE is_delete = 'T'
    );

    DELETE FROM research_project_funding
    WHERE research_project_funding_id IN (
      SELECT
        research_project_funding_id
      FROM im_research_project_funding
      WHERE is_delete = 'T'
    );

    DELETE FROM research_project_cohort
    WHERE research_project_cohort_id IN (
      SELECT
        research_project_cohort_id
      FROM im_research_project_cohort
      WHERE is_delete = 'T'
    );

    DELETE FROM research_project_irb
    WHERE research_project_irb_id IN (
      SELECT
        research_project_irb_id
      FROM im_research_project_irb
      WHERE is_delete = 'T'
    );

    DELETE FROM product_order_add_on
    WHERE product_order_add_on_id IN (
      SELECT
        product_order_add_on_id
      FROM im_product_order_add_on
      WHERE is_delete = 'T'
    );

    DELETE FROM price_item
    WHERE price_item_id IN (
      SELECT
        price_item_id
      FROM im_price_item
      WHERE is_delete = 'T'
    );

    DELETE FROM product_order
    WHERE product_order_id IN (
      SELECT
        product_order_id
      FROM im_product_order
      WHERE is_delete = 'T'
    );

    DELETE FROM lab_vessel
    WHERE lab_vessel_id IN (
      SELECT
        lab_vessel_id
      FROM im_lab_vessel
      WHERE is_delete = 'T'
    );

    DELETE FROM lab_metric
    WHERE lab_metric_id IN (SELECT lab_metric_id FROM im_lab_metric WHERE is_delete = 'T');

    DELETE FROM product
    WHERE product_id IN (
      SELECT
        product_id
      FROM im_product
      WHERE is_delete = 'T'
    );

    DELETE FROM research_project
    WHERE research_project_id IN (
      SELECT
        research_project_id
      FROM im_research_project
      WHERE is_delete = 'T'
    );

    DELETE FROM sequencing_run
    WHERE sequencing_run_id IN (
      SELECT
        sequencing_run_id
      FROM im_sequencing_run
      WHERE is_delete = 'T'
    );

    DELETE FROM ledger_entry
    WHERE ledger_id IN (
      SELECT
        ledger_id
      FROM im_ledger_entry
      WHERE is_delete = 'T'
    );

    DELETE FROM billing_session
    WHERE billing_session_id IN (
      SELECT
        billing_session_id
      FROM im_billing_session
      WHERE is_delete = 'T'
    );

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

    COMMIT;

-----------------------------------------------------------------------------------------
-- Updates rows when they exist in the target table, inserts rows when they do not exist.
-----------------------------------------------------------------------------------------

    FOR new IN im_rp_cur LOOP
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
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_research_project.dat line ' || new.line_number || '  ' ||
          errmsg);
      CONTINUE;
    END;

    END LOOP;


    FOR new IN im_price_item_cur LOOP
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
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_price_item.dat line ' || new.line_number || '  ' || errmsg);
      CONTINUE;
    END;

    END LOOP;


    FOR new IN im_product_cur LOOP
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
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product.dat line ' || new.line_number || '  ' || errmsg);
      CONTINUE;
    END;

    END LOOP;


    FOR new IN im_lab_vessel_cur LOOP
    BEGIN
      UPDATE lab_vessel
      SET
        lab_vessel_id = new.lab_vessel_id,
        label = new.label,
        lab_vessel_type = new.lab_vessel_type,
        etl_date = new.etl_date
      WHERE lab_vessel_id = new.lab_vessel_id;

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
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_lab_vessel.dat line ' || new.line_number || '  ' || errmsg);
      CONTINUE;
    END;

    END LOOP;


    FOR new IN im_lab_metric_cur LOOP
    BEGIN

      UPDATE lab_metric
      SET
        sample_name = new.sample_name,
        lab_vessel_id = new.lab_vessel_id,
        product_order_id = new.product_order_id,
        batch_name = new.batch_name,
        quant_type = new.quant_type,
        quant_units = new.quant_units,
        quant_value = new.quant_value,
        run_name = new.run_name,
        run_date = new.run_date,
        vessel_position = new.vessel_position,
        etl_date = new.etl_date
      WHERE lab_metric_id = new.lab_metric_id;

      INSERT INTO lab_metric (
        lab_metric_id,
        sample_name,
        lab_vessel_id,
        product_order_id,
        batch_name,
        quant_type,
        quant_units,
        quant_value,
        run_name,
        run_date,
        vessel_position,
        etl_date
      )
      SELECT
        new.lab_metric_id,
        new.sample_name,
        new.lab_vessel_id,
        new.product_order_id,
        new.batch_name,
        new.quant_type,
        new.quant_units,
        new.quant_value,
        new.run_name,
        new.run_date,
        new.vessel_position,
        new.etl_date
      FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM lab_metric WHERE lab_metric_id = new.lab_metric_id);

      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_lab_metric.dat line ' || new.line_number || '  ' || errmsg);
      CONTINUE;
    END;

    END LOOP;


    FOR new IN im_workflow_cur LOOP
    BEGIN
      UPDATE workflow
      SET
        workflow_id = new.workflow_id,
        workflow_name = new.workflow_name,
        workflow_version = new.workflow_version,
        etl_date = new.etl_date
      WHERE workflow_id = new.workflow_id;

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
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_workflow.dat line ' || new.line_number || '  ' || errmsg);
      CONTINUE;
    END;

    END LOOP;

    FOR new IN im_workflow_process_cur LOOP
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
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_workflow_process.dat line ' || new.line_number || '  ' ||
          errmsg);
      CONTINUE;
    END;

    END LOOP;


    FOR new IN im_sequencing_run_cur LOOP
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
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_sequencing_run.dat line ' || new.line_number || '  ' ||
          errmsg);
      CONTINUE;
    END;

    END LOOP;


    FOR new IN im_po_cur LOOP
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
        etl_date = new.etl_date
      WHERE product_order_id = new.product_order_id;

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
        etl_date
      )
        SELECT
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
          new.etl_date
        FROM DUAL
        WHERE NOT EXISTS(
            SELECT
              1
            FROM product_order
            WHERE product_order_id = new.product_order_id
        );
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order.dat line ' || new.line_number || '  ' || errmsg);
      CONTINUE;
    END;

    END LOOP;


    FOR new IN im_po_add_on_cur LOOP
    BEGIN
      UPDATE product_order_add_on
      SET
        product_order_id = new.product_order_id,
        product_id = new.product_id,
        etl_date = new.etl_date
      WHERE product_order_add_on_id = new.product_order_add_on_id;

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
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order_add_on.dat line ' || new.line_number || '  ' ||
          errmsg);
      CONTINUE;
    END;

    END LOOP;


    FOR new IN im_rp_status_cur LOOP
    BEGIN
      UPDATE research_project_status
      SET
        status_date = new.status_date,
        status = new.status,
        etl_date = new.etl_date
      WHERE research_project_id = new.research_project_id
            AND status_date = new.status_date;

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
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_research_project_status.dat line ' || new.line_number || '  '
          || errmsg);
      CONTINUE;
    END;

    END LOOP;


    FOR new IN im_rp_person_cur LOOP
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
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_research_project_person.dat line ' || new.line_number || '  '
          || errmsg);
      CONTINUE;
    END;

    END LOOP;


    FOR new IN im_rp_funding_cur LOOP
    BEGIN
      UPDATE research_project_funding
      SET
        funding_id = new.funding_id,
        etl_date = new.etl_date
      WHERE research_project_funding_id = new.research_project_funding_id;

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
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_research_project_funding.dat line ' || new.line_number || '  '
          || errmsg);
      CONTINUE;
    END;

    END LOOP;


    FOR new IN im_rp_cohort_cur LOOP
    BEGIN
      UPDATE research_project_cohort
      SET
        etl_date = new.etl_date
      WHERE research_project_cohort_id = new.research_project_cohort_id;

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
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_research_project_cohort.dat line ' || new.line_number || '  '
          || errmsg);
      CONTINUE;
    END;

    END LOOP;


    FOR new IN im_rp_irb_cur LOOP
    BEGIN
      UPDATE research_project_irb
      SET
        research_project_id = new.research_project_id,
        research_project_irb = new.research_project_irb,
        research_project_irb_type = new.research_project_irb_type,
        etl_date = new.etl_date
      WHERE research_project_irb_id = new.research_project_irb_id;

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
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_research_project_irb.dat line ' || new.line_number || '  ' ||
          errmsg);
      CONTINUE;
    END;

    END LOOP;


    FOR new IN im_po_sample_cur LOOP
    BEGIN

      UPDATE product_order_sample
      SET
        product_order_id = new.product_order_id,
        sample_name = new.sample_name,
        delivery_status = new.delivery_status,
        sample_position = new.sample_position,
        etl_date = new.etl_date
      WHERE product_order_sample_id = new.product_order_sample_id;

      INSERT INTO product_order_sample (
        product_order_sample_id,
        product_order_id,
        sample_name,
        delivery_status,
        sample_position,
        etl_date
      )
        SELECT
          new.product_order_sample_id,
          new.product_order_id,
          new.sample_name,
          new.delivery_status,
          new.sample_position,
          new.etl_date
        FROM DUAL
        WHERE NOT EXISTS(
            SELECT
              1
            FROM product_order_sample
            WHERE product_order_sample_id = new.product_order_sample_id
        );
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order_sample.dat line ' || new.line_number || '  ' ||
          errmsg);
      CONTINUE;
    END;

    END LOOP;

    -- Only sets PK when it is null, so we can do an idempotent repeatable merge.
    UPDATE im_event_fact
    SET event_fact_id = event_fact_id_seq.nextval
    WHERE event_fact_id IS NULL;

    FOR new IN im_event_fact_cur LOOP
    BEGIN

      BEGIN
        -- Raises exception for an invalid batch_name if not a BSP workflow.
        SELECT 1 INTO v_tmp FROM DUAL
        WHERE NVL(new.batch_name, 'NONE') NOT IN ('NONE', 'MULTIPLE')
        OR EXISTS (SELECT 1 FROM workflow w
        WHERE w.workflow_name = 'BSP' AND new.workflow_id = w.workflow_id)
        OR EXISTS (SELECT 1 FROM workflow_process p
        WHERE p.event_name = 'PicoPlatingBucket' AND new.process_id = p.process_id);

        EXCEPTION WHEN NO_DATA_FOUND
        THEN RAISE INVALID_LAB_BATCH;
      END;


    -- No update is possible due to lack of common unique key

      INSERT INTO event_fact (
        event_fact_id,
        lab_event_id,
        workflow_id,
        process_id,
        product_order_id,
        sample_name,
        batch_name,
        station_name,
        lab_vessel_id,
        event_date,
        etl_date,
        program_name
      )
        SELECT
          new.event_fact_id,
          new.lab_event_id,
          new.workflow_id,
          new.process_id,
          new.product_order_id,
          new.sample_name,
          new.batch_name,
          new.station_name,
          new.lab_vessel_id,
          new.event_date,
          new.etl_date,
          new.program_name
        FROM DUAL
        WHERE NOT EXISTS(
            SELECT
              1
            FROM event_fact
            WHERE event_fact_id = new.event_fact_id
        );

      EXCEPTION

      WHEN INVALID_LAB_BATCH THEN
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_event_fact.dat line ' || new.line_number || '  ' ||
          'Event fact has invalid lab batch name: ' || NVL(new.batch_name, 'NONE'));
      CONTINUE;

      WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_event_fact.dat line ' || new.line_number || '  ' || errmsg);
      CONTINUE;
    END;

    END LOOP;


--Level 3 (depends on level 2 tables)


    FOR new IN im_po_status_cur LOOP
    BEGIN
      UPDATE product_order_status
      SET
        status = new.status,
        etl_date = new.etl_date
      WHERE product_order_id = new.product_order_id
            AND status_date = new.status_date;

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
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order_status.dat line ' || new.line_number || '  ' ||
          errmsg);
      CONTINUE;
    END;

    END LOOP;


    FOR new IN im_po_sample_stat_cur LOOP
    BEGIN
      UPDATE product_order_sample_status
      SET
        delivery_status = new.delivery_status,
        etl_date = new.etl_date
      WHERE product_order_sample_id = new.product_order_sample_id
            AND status_date = new.status_date;

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
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order_sample_status.dat line ' || new.line_number ||
          '  ' || errmsg);
      CONTINUE;
    END;

    END LOOP;

    FOR new IN im_po_sample_risk_cur LOOP
    BEGIN

      UPDATE product_order_sample
      SET
        on_risk = new.on_risk,
        risk_types = new.risk_types,
        risk_messages = new.risk_messages
      WHERE product_order_sample_id = new.product_order_sample_id;

      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order_sample_risk.dat line ' || new.line_number || '  '
          || errmsg);
      CONTINUE;
    END;

    END LOOP;

    FOR new IN im_po_sample_bill_cur LOOP
    BEGIN

      UPDATE product_order_sample
      SET
        is_billed = new.is_billed
      WHERE product_order_sample_id = new.product_order_sample_id;

      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_product_order_sample_bill.dat line ' || new.line_number || '  '
          || errmsg);
      CONTINUE;
    END;

    END LOOP;

    FOR new IN im_billing_session_cur LOOP
    BEGIN
      UPDATE billing_session
      SET
        billing_session_id = new.billing_session_id,
        billed_date = new.billed_date,
        billing_session_type = new.billing_session_type,
        etl_date = new.etl_date
      WHERE billing_session_id = new.billing_session_id;

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
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_billing_session.dat line ' || new.line_number ||
          '  ' || errmsg);
      CONTINUE;
    END;
    END LOOP;

    -- Loop for cross entity ETL.
    FOR new IN im_ledger_entry_cur LOOP
    BEGIN

      UPDATE product_order_sample
      SET
        ledger_quote_id = new.quote_id
      WHERE product_order_sample_id = new.product_order_sample_id;

      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_ledger_entry.dat line ' || new.line_number || '  ' || errmsg);
      CONTINUE;
    END;

    END LOOP;

    FOR new IN im_ledger_entry_cur LOOP
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
      EXCEPTION WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_ledger_entry.dat line ' || new.line_number ||
          '  ' || errmsg);
      CONTINUE;
    END;
    END LOOP;

-- Only sets PK when it is null, so we can do an idempotent repeatable merge.
    UPDATE im_sequencing_sample_fact
    SET sequencing_sample_fact_id = sequencing_sample_id_seq.nextval
    WHERE sequencing_sample_fact_id IS NULL;

    FOR new IN im_seq_sample_fact_cur LOOP
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
        THEN RAISE PDO_SAMPLE_NOT_IN_EVENT_FACT;
      END;

      BEGIN
        -- Reports an invalid batch_name.
        SELECT 1 INTO v_tmp FROM DUAL
        WHERE NVL(new.batch_name, 'NONE') NOT IN ('NONE', 'MULTIPLE');

        EXCEPTION WHEN NO_DATA_FOUND
        THEN RAISE INVALID_LAB_BATCH;
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
      EXCEPTION

      WHEN PDO_SAMPLE_NOT_IN_EVENT_FACT THEN
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_sequencing_sample_fact.dat line ' || new.line_number || '  ' ||
          'Sequencing Fact sample and product order not found in Event_Fact table');
      CONTINUE;

      WHEN INVALID_LAB_BATCH THEN
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_sequencing_sample_fact.dat line ' || new.line_number || '  ' ||
          'Sequencing Fact has invalid lab batch name: ' || NVL(new.batch_name, 'NONE'));
      CONTINUE;

      WHEN OTHERS THEN
      errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_sequencing_sample_fact.dat line ' || new.line_number || '  ' ||
          errmsg);
      CONTINUE;
    END;

    END LOOP;


    COMMIT;

  END;
