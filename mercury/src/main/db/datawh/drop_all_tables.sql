----------------------------------------
-- do not run this on the production db
----------------------------------------
CREATE OR REPLACE PROCEDURE drop_all_tables
IS
BEGIN

 FOR i IN (
   SELECT * FROM all_objects
   WHERE owner = 'MERCURYDW'
   AND object_type = 'SEQUENCE'
 )
 LOOP
   execute immediate 'DROP '||i.object_type||' '||i.object_name;
 END LOOP;

 FOR i IN (
   SELECT * FROM all_objects
   WHERE owner = 'MERCURYDW'
   AND object_type IN ( 'TABLE', 'VIEW' )
 )
 LOOP
   execute immediate 'DROP '||i.object_type||' '||i.object_name||' CASCADE CONSTRAINTS';
 END LOOP;

END drop_all_tables;
