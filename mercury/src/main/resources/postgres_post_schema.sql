-- hbm2ddl generates a column named mapkey, with a type of bytea, but it should be varchar:
alter table molecular_index_position alter column MAPKEY type varchar(50);
alter table lv_map_position_to_vessel alter column MAPKEY type varchar(50);
create index ix_ssss_sample_sheets on sample_sheet_starting_samples(starting_samples);