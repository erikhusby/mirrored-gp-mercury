/*
 Does insert/update/delete to all reporting tables, using data from import tables.
 Tables are put into one of 3 groups depending on their FK dependencies.
 All tables within a grouping can be (but are not yet) processed in parallel.
*/
CREATE OR REPLACE PROCEDURE merge_import
IS

CURSOR im_po_cur IS SELECT * FROM im_product_order WHERE is_delete = 'F';
CURSOR im_po_sample_cur IS SELECT * FROM im_product_order_sample WHERE is_delete = 'F';
CURSOR im_po_status_cur IS SELECT * FROM im_product_order_status WHERE is_delete = 'F';
CURSOR im_po_sample_stat_cur IS SELECT * FROM im_product_order_sample_stat WHERE is_delete = 'F';
CURSOR im_price_item_cur IS SELECT * FROM im_price_item WHERE is_delete = 'F';
CURSOR im_po_add_on_cur IS SELECT * FROM im_product_order_add_on WHERE is_delete = 'F';
CURSOR im_product_cur IS SELECT * FROM im_product WHERE is_delete = 'F';
CURSOR im_rp_cohort_cur IS SELECT * FROM im_research_project_cohort WHERE is_delete = 'F';
CURSOR im_rp_cur IS SELECT * FROM im_research_project WHERE is_delete = 'F';
CURSOR im_rp_funding_cur IS SELECT * FROM im_research_project_funding WHERE is_delete = 'F';
CURSOR im_rp_irb_cur IS SELECT * FROM im_research_project_irb WHERE is_delete = 'F';
CURSOR im_rp_person_cur IS SELECT * FROM im_research_project_person WHERE is_delete = 'F';
CURSOR im_rp_status_cur IS SELECT * FROM im_research_project_status WHERE is_delete = 'F';

errmsg VARCHAR2(255);

BEGIN

---------------------------------------------------------------------------
-- Deletes rows in the reporting tables when the import has is_delete = 'T'.
-- Does the most dependent (FK dependency) tables first.
---------------------------------------------------------------------------

--Level 3 (depends on level 2 tables)

DELETE FROM product_order_status
WHERE product_order_id IN (
  SELECT product_order_id FROM im_product_order_status WHERE is_delete = 'T'
);

DELETE FROM product_order_sample_status
WHERE product_order_sample_id IN (
  SELECT product_order_sample_id FROM im_product_order_sample_stat WHERE is_delete = 'T'
);

DELETE FROM product_order_add_on
WHERE product_order_add_on_id IN (
  SELECT product_order_add_on_id FROM im_product_order_add_on WHERE is_delete = 'T'
);

--Level 2 (depends only on level 1 tables)

DELETE FROM research_project_status
WHERE research_project_id IN (
  SELECT research_project_id FROM im_research_project_status WHERE is_delete = 'T'
);
 
DELETE FROM research_project_person
WHERE research_project_person_id IN (
  SELECT research_project_person_id FROM im_research_project_person WHERE is_delete = 'T'
);

DELETE FROM research_project_funding
WHERE research_project_funding_id IN (
  SELECT research_project_funding_id FROM im_research_project_funding WHERE is_delete = 'T'
);

DELETE FROM research_project_cohort
WHERE research_project_cohort_id IN (
  SELECT research_project_cohort_id FROM im_research_project_cohort WHERE is_delete = 'T'
);

DELETE FROM research_project_irb
WHERE research_project_irb_id IN (
  SELECT research_project_irb_id FROM im_research_project_irb WHERE is_delete = 'T'
);

DELETE FROM product_order
WHERE product_order_id IN (
  SELECT product_order_id FROM im_product_order WHERE is_delete = 'T'
);

DELETE FROM product_order_sample
WHERE product_order_sample_id IN (
  SELECT product_order_sample_id FROM im_product_order_sample WHERE is_delete = 'T'
);

-- Level 1 (independent tables)

DELETE FROM research_project
WHERE research_project_id IN (
  SELECT research_project_id FROM im_research_project WHERE is_delete = 'T'
);

DELETE FROM price_item
WHERE price_item_id IN (
  SELECT price_item_id FROM im_price_item WHERE is_delete = 'T'
);

DELETE FROM product
WHERE product_id IN (
  SELECT product_id FROM im_product WHERE is_delete = 'T'
);

COMMIT;

-----------------------------------------------------------------------------------------
-- Updates rows when they exist in the target table, inserts rows when they do not exist.
-----------------------------------------------------------------------------------------

-- Level 1 (independent tables)

FOR new IN im_rp_cur LOOP
  BEGIN
    UPDATE research_project SET
      current_status = new.current_status,
      created_date = new.created_date,
      title = new.title,
      irb_not_engaged = new.irb_not_engaged,
      jira_ticket_key = new.jira_ticket_key,
      etl_date = new.etl_date
    WHERE research_project_id = new.research_project_id;

    INSERT INTO research_project (
      research_project_id,
      current_status,
      created_date,
      title,
      irb_not_engaged,
      jira_ticket_key,
      etl_date
    )
    SELECT
      new.research_project_id,
      new.current_status,
      new.created_date,
      new.title,
      new.irb_not_engaged,
      new.jira_ticket_key,
      new.etl_date
    FROM DUAL WHERE NOT EXISTS (
      SELECT 1 FROM research_project
      WHERE research_project_id = new.research_project_id
    );
  EXCEPTION WHEN OTHERS THEN
    errmsg := SQLERRM;
    DBMS_OUTPUT.PUT_LINE(TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS')||'_research_project.dat line '||new.line_number||'  '||errmsg);
    CONTINUE;
  END;

END LOOP;


FOR new IN im_product_cur LOOP
  BEGIN
    UPDATE product SET
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
      new.etl_date
    FROM DUAL WHERE NOT EXISTS (
      SELECT 1 FROM product
      WHERE product_id = new.product_id
    );
  EXCEPTION WHEN OTHERS THEN 
    errmsg := SQLERRM;
    DBMS_OUTPUT.PUT_LINE(TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS')||'_product.dat line '||new.line_number||'  '||errmsg);
    CONTINUE;
  END;

END LOOP;


--Level 2 (depends only on level 1 tables)

FOR new IN im_po_cur LOOP
  BEGIN
    UPDATE product_order SET
      research_project_id = new.research_project_id,
      product_id = new.product_id,
      status = new.status,
      created_date = new.created_date,
      modified_date = new.modified_date,
      title = new.title,
      quote_id = new.quote_id,
      jira_ticket_key = new.jira_ticket_key,
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
      new.etl_date
    FROM DUAL WHERE NOT EXISTS (
      SELECT 1 FROM product_order
      WHERE product_order_id = new.product_order_id
    );
  EXCEPTION WHEN OTHERS THEN 
    errmsg := SQLERRM;
    DBMS_OUTPUT.PUT_LINE(TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS')||'_product_order.dat line '||new.line_number||'  '||errmsg);
    CONTINUE;
  END;

END LOOP;

FOR new IN im_price_item_cur LOOP
  BEGIN
    UPDATE price_item SET
      platform = new.platform,
      category = new.category,
      price_item_name = new.price_item_name,
      quote_server_id = new.quote_server_id,
      price = new.price,
      units = new.units,
      etl_date = new.etl_date
    WHERE  price_item_id = new.price_item_id;

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
    FROM DUAL WHERE NOT EXISTS (
      SELECT 1 FROM price_item
      WHERE price_item_id = new.price_item_id
    );
  EXCEPTION WHEN OTHERS THEN 
    errmsg := SQLERRM;
    DBMS_OUTPUT.PUT_LINE(TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS')||'_price_item.dat line '||new.line_number||'  '||errmsg);
    CONTINUE;
  END;

END LOOP;


FOR new IN im_po_add_on_cur  LOOP
  BEGIN
    UPDATE product_order_add_on SET
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
    FROM DUAL WHERE NOT EXISTS (
      SELECT 1 FROM product_order_add_on
      WHERE product_order_add_on_id = new.product_order_add_on_id
    );
  EXCEPTION WHEN OTHERS THEN 
    errmsg := SQLERRM;
    DBMS_OUTPUT.PUT_LINE(TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS')||'_product_order_add_on.dat line '||new.line_number||'  '||errmsg);
    CONTINUE;
  END;

END LOOP;


FOR new IN im_rp_status_cur LOOP
  BEGIN
    UPDATE research_project_status SET
      status_date = new.status_date,
      status = new.status,
      etl_date = new.etl_date
    WHERE  research_project_id = new.research_project_id
    AND    status_date = new.status_date;

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
    FROM DUAL WHERE NOT EXISTS (
      SELECT 1 FROM research_project_status
      WHERE  research_project_id = new.research_project_id
      AND    status_date = new.status_date
    );
  EXCEPTION WHEN OTHERS THEN 
    errmsg := SQLERRM;
    DBMS_OUTPUT.PUT_LINE(TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS')||'_research_project_status.dat line '||new.line_number||'  '||errmsg);
    CONTINUE;
  END;

END LOOP;


FOR new IN im_rp_person_cur LOOP
  BEGIN
    UPDATE research_project_person SET
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
    FROM DUAL WHERE NOT EXISTS (
      SELECT 1 FROM research_project_person
      WHERE research_project_person_id = new.research_project_person_id
    );
  EXCEPTION WHEN OTHERS THEN 
    errmsg := SQLERRM;
    DBMS_OUTPUT.PUT_LINE(TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS')||'_research_project_person.dat line '||new.line_number||'  '||errmsg);
    CONTINUE;
  END;

END LOOP;


FOR new IN im_rp_funding_cur LOOP
  BEGIN
    UPDATE research_project_funding SET
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
    FROM DUAL WHERE NOT EXISTS (
      SELECT 1 FROM research_project_funding
      WHERE research_project_funding_id = new.research_project_funding_id
    );
  EXCEPTION WHEN OTHERS THEN 
    errmsg := SQLERRM;
    DBMS_OUTPUT.PUT_LINE(TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS')||'_research_project_funding.dat line '||new.line_number||'  '||errmsg);
    CONTINUE;
  END;

END LOOP;


FOR new IN im_rp_cohort_cur LOOP
  BEGIN
    UPDATE research_project_cohort SET
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
    FROM DUAL WHERE NOT EXISTS (
      SELECT 1 FROM research_project_cohort
      WHERE research_project_cohort_id = new.research_project_cohort_id
    );
  EXCEPTION WHEN OTHERS THEN 
    errmsg := SQLERRM;
    DBMS_OUTPUT.PUT_LINE(TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS')||'_research_project_cohort.dat line '||new.line_number||'  '||errmsg);
    CONTINUE;
  END;

END LOOP;


FOR new IN im_rp_irb_cur LOOP
  BEGIN
    UPDATE research_project_irb SET
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
    FROM DUAL WHERE NOT EXISTS (
      SELECT 1 FROM research_project_irb
      WHERE research_project_irb_id = new.research_project_irb_id
    );
  EXCEPTION WHEN OTHERS THEN 
    errmsg := SQLERRM;
    DBMS_OUTPUT.PUT_LINE(TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS')||'_research_project_irb.dat line '||new.line_number||'  '||errmsg);
    CONTINUE;
  END;

END LOOP;


FOR new IN im_po_sample_cur LOOP
  BEGIN
    UPDATE product_order_sample SET
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
    FROM DUAL WHERE NOT EXISTS (
      SELECT 1 FROM product_order_sample
      WHERE product_order_sample_id = new.product_order_sample_id
    );
  EXCEPTION WHEN OTHERS THEN 
    errmsg := SQLERRM;
    DBMS_OUTPUT.PUT_LINE(TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS')||'_product_order_sample.dat line '||new.line_number||'  '||errmsg);
    CONTINUE;
  END;

END LOOP;



--Level 3 (depends on level 2 tables)


FOR new IN im_po_status_cur LOOP
  BEGIN
    UPDATE product_order_status SET
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
    FROM DUAL WHERE NOT EXISTS (
      SELECT 1 FROM product_order_status
      WHERE product_order_id = new.product_order_id
      AND status_date = new.status_date
    );
  EXCEPTION WHEN OTHERS THEN 
    errmsg := SQLERRM;
    DBMS_OUTPUT.PUT_LINE(TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS')||'_product_order_status.dat line '||new.line_number||'  '||errmsg);
    CONTINUE;
  END;

END LOOP;


FOR new IN im_po_sample_stat_cur LOOP
  BEGIN
    UPDATE product_order_sample_status SET
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
    FROM DUAL WHERE NOT EXISTS (
      SELECT 1 FROM product_order_sample_status
      WHERE product_order_sample_id = new.product_order_sample_id
      AND status_date = new.status_date
    );
  EXCEPTION WHEN OTHERS THEN 
    errmsg := SQLERRM;
    DBMS_OUTPUT.PUT_LINE(TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS')||'_product_order_sample_status.dat line '||new.line_number||'  '||errmsg);
    CONTINUE;
  END;

END LOOP;


COMMIT;

END;
