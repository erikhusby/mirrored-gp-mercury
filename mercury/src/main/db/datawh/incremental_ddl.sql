-- --------------------------------
-- https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-5455
-- Deletes duplicate lab events (unique by lab_event_id, ef.sample_name, lcset_sample_name, and ef.position)
-- --------------------------------
set serveroutput on size unlimited;

declare
  V_ERRMSG varchar2(100);

  v_row_count number(7);
  v_del_count number(7);

  v_max_id number(10);

  v_batch_start_id number(10);
  v_batch_end_id number(10);

  cursor cur_dups( a_start number, a_end number ) is
    SELECT max(rowid) as rowtokeep, ef.lab_event_id, NVL(ef.sample_name, ' '), NVL(ef.lcset_sample_name, ' '), NVL(ef.position, ' ')
      FROM mercurydw.event_fact ef
     WHERE ef.lab_event_id between a_start and a_end
    GROUP BY ef.lab_event_id, NVL(ef.sample_name, ' '), NVL(ef.lcset_sample_name, ' '), NVL(ef.position, ' ')
    HAVING count(*) > 1;

  type rowid_arr_ty is table of rowid index by pls_integer;
  v_rowid_arr rowid_arr_ty;

  type nbr_arr_ty is table of number(10) index by pls_integer;
  v_id_arr nbr_arr_ty;

  type vchar_arr_ty is table of varchar2(40) index by pls_integer;
  v_sample_arr vchar_arr_ty;
  v_lcsetsample_arr vchar_arr_ty;
  v_position_arr vchar_arr_ty;
begin

  v_row_count := 0;
  v_del_count := 0;

  select max(lab_event_id) into v_max_id from event_fact;

  v_batch_start_id := 0;

  while v_batch_start_id < v_max_id loop
    v_batch_end_id := v_batch_start_id + 10000;

    dbms_output.put_line( 'Batching lab event ids between ' || v_batch_start_id || ' and ' || v_batch_end_id );

    open cur_dups( v_batch_start_id, v_batch_end_id );
    fetch cur_dups bulk collect into v_rowid_arr, v_id_arr, v_sample_arr, v_lcsetsample_arr, v_position_arr;
    close cur_dups;

    if v_rowid_arr.count > 0 then
      begin
        savepoint this_batch;

        forall idx in v_rowid_arr.first .. v_rowid_arr.last
        delete from event_fact
        where lab_event_id                = v_id_arr(idx)
          and NVL(sample_name, ' ')       = v_sample_arr(idx)
          and NVL(lcset_sample_name, ' ') = v_lcsetsample_arr(idx)
          and NVL(position, ' ')          = v_position_arr(idx)
          and rowid <> v_rowid_arr(idx);

        v_del_count := v_del_count + sql%ROWCOUNT;

        commit;

        dbms_output.put_line( '.  Deleted ' || v_del_count || ' rows in batch' );

      exception when others then
        V_ERRMSG := SUBSTR(SQLERRM, 1, 100);
        rollback to this_batch;
        dbms_output.put_line( '.  Batch fail: ' || V_ERRMSG );
      end;

    else
      dbms_output.put_line( '.  No duplicate rows in batch' );
    end if;

    v_batch_start_id := v_batch_end_id + 1;

  end loop;

end;
/



