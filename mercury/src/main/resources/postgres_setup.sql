--password is mercury_local
CREATE ROLE mercury LOGIN ENCRYPTED PASSWORD 'md5c95f4bb87c013c7d6654015080802f87'
  CREATEDB
   VALID UNTIL 'infinity';

CREATE SCHEMA mercury
       AUTHORIZATION mercury;
