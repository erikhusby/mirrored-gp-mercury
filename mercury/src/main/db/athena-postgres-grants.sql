
GRANT USAGE ON SCHEMA athena TO mercury;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA athena TO mercury;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA athena TO mercury;

GRANT SELECT, INSERT, UPDATE, DELETE ON mercury.rev_info to athena;
GRANT USAGE on mercury.seq_rev_info to athena;
