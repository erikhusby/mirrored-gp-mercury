-- Creates table synonyms in mercurydwro.
-- This should be run by mercurydwro every time a new warehouse table is created.
CREATE OR REPLACE PROCEDURE make_dwro_synonyms
IS
BEGIN
  FOR i IN (
    SELECT * FROM ALL_OBJECTS o1
    WHERE o1.OWNER = 'MERCURYDW'
    AND o1.OBJECT_TYPE IN ( 'TABLE', 'VIEW' )
    AND o1.OBJECT_NAME NOT LIKE 'IM_%'
    AND NOT EXISTS
    (SELECT 1 FROM ALL_OBJECTS o2
     WHERE o2.OWNER = 'MERCURYDWRO'
     AND o2.OBJECT_TYPE = 'SYNONYM'
     AND o2.OBJECT_NAME = o1.OBJECT_NAME)
  )
  LOOP
    EXECUTE IMMEDIATE 'create or replace synonym ' || i.object_name || ' for MERCURYDW.' || i.object_name;
  END LOOP;
END;
