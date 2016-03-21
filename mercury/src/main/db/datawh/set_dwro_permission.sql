-- Permits mercurydwro to read mercurydw tables.
-- This should be run by mercurydw every time a new warehouse table is created.
CREATE OR REPLACE PROCEDURE set_dwro_permission
IS
BEGIN
  FOR i IN (SELECT * FROM ALL_OBJECTS o1
            WHERE o1.OWNER = 'MERCURYDW'
            AND o1.OBJECT_TYPE IN ( 'TABLE', 'VIEW' )
            AND o1.OBJECT_NAME NOT LIKE 'IM_%')
  LOOP
    EXECUTE IMMEDIATE 'grant select on ' || i.object_name || ' to MERCURYDWRO';
  END LOOP;
END;
