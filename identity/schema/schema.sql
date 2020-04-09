CREATE SCHEMA identity;

CREATE TABLE identity.role (
  id SERIAL,
  role CHAR(10) NOT NULL UNIQUE
) ENGINE INNODB;

CREATE TABLE identity.identity (
  id SERIAL,
  user VARCHAR(20) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  role_id BIGINT(20) UNSIGNED NOT NULL,
  FOREIGN KEY (role_id) REFERENCES identity.role(id) ON DELETE CASCADE
) ENGINE INNODB;

CREATE UNIQUE INDEX by_user ON identity.identity(user);

-- We have default values here.

INSERT INTO identity.role(role) VALUES ('admin'), ('media'), ('user');
