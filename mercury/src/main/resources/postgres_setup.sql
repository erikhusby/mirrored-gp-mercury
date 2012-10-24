--password is mercury_local
CREATE ROLE mercury LOGIN ENCRYPTED PASSWORD 'md5c95f4bb87c013c7d6654015080802f87'
  CREATEDB
   VALID UNTIL 'infinity';

CREATE SCHEMA mercury
       AUTHORIZATION mercury;

--password is athena_local
CREATE ROLE athena LOGIN ENCRYPTED PASSWORD 'md5fac80c91a15c3a4a610fe4f62384a977'
  CREATEDB
   VALID UNTIL 'infinity';

CREATE SCHEMA athena
       AUTHORIZATION athena;

-- This function is useful for generating grant statements for many tables
create function execute(text) returns void as $BODY$BEGIN execute $1; END;$BODY$ language plpgsql;
