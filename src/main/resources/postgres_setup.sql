--password is sequel_local
CREATE ROLE sequel LOGIN ENCRYPTED PASSWORD 'md5a2a36fd51f14a52a5fab711046947b7f'
  CREATEDB
   VALID UNTIL 'infinity';

CREATE SCHEMA sequel
       AUTHORIZATION sequel;
