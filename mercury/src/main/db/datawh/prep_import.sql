/*
 Truncates import tables.
*/
CREATE OR REPLACE PROCEDURE prep_import
IS
BEGIN

  for i in (
    select * from all_objects
    where owner = 'MERCURYDW'
    and object_type = 'TABLE'
    and object_name like 'IM_%'
  )
  loop
    execute immediate 'TRUNCATE TABLE '||i.object_name;
  end loop;

  COMMIT;
END prep_import;

