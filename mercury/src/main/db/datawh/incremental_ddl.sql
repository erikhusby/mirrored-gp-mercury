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
      FOR cur_rec IN ( SELECT DISTINCT COLUMN_VALUE AS token
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

  IF sort_types('C AND A AND A') <> 'A AND C' THEN
    RAISE_APPLICATION_ERROR( -20001, '*** SORT FAIL > [C AND A AND A]');
  END IF;

  IF sort_types('A AND C AND B') <> 'A AND B AND C' THEN
    RAISE_APPLICATION_ERROR( -20001, '*** SORT FAIL > [A AND C AND B]');
  END IF;

  IF sort_types('Is FFPE AND Concentration AND Total DNA') <> 'Concentration AND Is FFPE AND Total DNA' THEN
    RAISE_APPLICATION_ERROR( -20001, '*** SORT FAIL > [Is FFPE AND Concentration AND Total DNA]');
  END IF;

  IF sort_types('Concentration AND Concentration') <> 'Concentration' THEN
    RAISE_APPLICATION_ERROR( -20001, '*** SORT FAIL > [Concentration AND Concentration]');
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
