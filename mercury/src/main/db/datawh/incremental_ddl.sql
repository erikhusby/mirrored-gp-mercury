-------------------------------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4168
-- Remove and prevent duplicate library ancestry relationships
SET SERVEROUTPUT ON SIZE UNLIMITED;

DECLARE
  TYPE NUM_ARR_TY IS TABLE OF NUMBER(19) INDEX BY PLS_INTEGER;
  EVENT_ARR NUM_ARR_TY;
  CHILD_AR NUM_ARR_TY;
  ANCEST_ARR NUM_ARR_TY;
BEGIN

  -- Delete duplicates associated with older lab events (by ID)
  -- About 2600 so no need to limit batch size
  SELECT min(la.child_event_id) as event_to_delete, la.child_library_id, la.ancestor_library_id
    BULK COLLECT INTO EVENT_ARR, CHILD_AR, ANCEST_ARR
    FROM library_ancestry la
  GROUP BY la.child_library_id, la.ancestor_library_id
  HAVING count( * ) > 1;

  FORALL IDX IN EVENT_ARR.FIRST .. EVENT_ARR.LAST
    DELETE FROM library_ancestry
     WHERE child_event_id = EVENT_ARR(IDX)
       AND child_library_id = CHILD_AR(IDX)
       AND ancestor_library_id = ANCEST_ARR(IDX);

  DBMS_OUTPUT.PUT_LINE('DELETED ' || SQL%ROWCOUNT || ' duplicate ancestry rows');
END;
/

COMMIT;

DROP INDEX PK_ANCESTRY;
CREATE UNIQUE INDEX PK_ANCESTRY on library_ancestry (child_library_id, ancestor_library_id ) compute statistics;
ALTER TABLE library_ancestry
ADD CONSTRAINT PK_ANCESTRY PRIMARY KEY (child_library_id, ancestor_library_id ) USING INDEX PK_ANCESTRY;

DROP INDEX idx_ancestry_reverse;
CREATE UNIQUE INDEX idx_ancestry_reverse on library_ancestry (ancestor_library_id, child_library_id ) compute statistics;

