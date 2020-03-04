CREATE SCHEMA auth;

CREATE TABLE auth.authorize (
  id SERIAL,
  user VARCHAR(20),
  password CHAR(64)
) ENGINE INNODB;

CREATE UNIQUE INDEX by_user ON auth.authorize(user);
