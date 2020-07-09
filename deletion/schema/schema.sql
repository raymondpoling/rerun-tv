CREATE SCHEMA deletion;

CREATE TABLE deletion.record (
  id SERIAL,
  name VARCHAR(150) NOT NULL,
  type VARCHAR(20) NOT NULL,
  user VARCHAR(20) NOT NULL,
  reason TEXT,
  status CHAR(3) NOT NULL
) ENGINE INNODB;

CREATE INDEX by_status ON deletion.record(status);

-- We have default values here.
-- Status:
-- NOM - nominated
-- REJ - rejected
-- EXE - executed
