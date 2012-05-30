-- USER SQL
CREATE USER sequel IDENTIFIED BY sequel_dev1
DEFAULT TABLESPACE "SQUID_NEW_DATA"
TEMPORARY TABLESPACE "TEMP";

-- ROLES

-- SYSTEM PRIVILEGES
GRANT CREATE SEQUENCE TO sequel ;
GRANT CREATE TABLE TO sequel ;
GRANT CREATE SESSION TO sequel ;
GRANT SELECT ANY DICTIONARY TO sequel ;

-- QUOTAS
ALTER USER "SEQUEL" QUOTA UNLIMITED ON SQUID_NEW_DATA;
