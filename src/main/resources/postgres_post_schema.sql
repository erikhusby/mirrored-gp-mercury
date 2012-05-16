-- hbm2ddl generates a column named mapkey, with a type of bytea, but it should be varchar:
alter table molecular_index_position alter column MAPKEY type varchar(50);