CREATE SCHEMA deletion;

CREATE TABLE deletion.record (
  id SERIAL,
  name VARCHAR(150) NOT NULL,
  type VARCHAR(20) NOT NULL,
  maker VARCHAR(20) NOT NULL,
  checker VARCHAR(20),
  reason1 TEXT,
  reason2 TEXT,
  status CHAR(3) NOT NULL
) ENGINE INNODB;

CREATE INDEX by_status ON deletion.record(status);

-- We have default values here.
-- Status:
-- NOM - nominated
-- REJ - rejected
-- EXE - executed

-- reason1 is for maker
-- reason2 is for checker
