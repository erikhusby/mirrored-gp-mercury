-- hbm2ddl generates a column named mapkey, with a type of raw, but it should be varchar2:
alter table molecular_index_position modify (MAPKEY varchar2(50) not null);
alter table lv_map_position_to_vessel modify (MAPKEY varchar2(50) not null);
