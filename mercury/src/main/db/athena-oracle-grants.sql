
DECLARE
    CURSOR c_tables IS
    SELECT
        table_name
    FROM
        user_tables;

    CURSOR c_sequences IS
    SELECT
        sequence_name
    FROM
        user_sequences;

    v_cmd VARCHAR2(200);
BEGIN
    FOR v_table IN c_tables
    LOOP
        v_cmd := 'GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON ' || v_table.table_name || ' TO MERCURY';
        EXECUTE IMMEDIATE v_cmd;
    END LOOP;
    FOR v_sequence IN c_sequences
    LOOP
        v_cmd := 'GRANT SELECT ON ' || v_sequence.sequence_name || ' TO MERCURY';
        EXECUTE IMMEDIATE v_cmd;
    END LOOP;
END;
