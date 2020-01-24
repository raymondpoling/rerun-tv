#CREATE SCHEMA playlist;

DROP TABLE IF EXISTS playlist.name;

#DROP INDEX by_names ON playlist.name;

DROP TABLE IF EXISTS playlist.playlist;

#DROP INDEX find_item ON playlist.playlist;

CREATE TABLE playlist.name (
  id SERIAL,
  name VARCHAR(12),
  PRIMARY KEY (id)
) ENGINE INNODB;

CREATE UNIQUE INDEX by_names ON playlist.name (name);

CREATE TABLE playlist.playlist (
  id SERIAL,
  name_key BIGINT(20) UNSIGNED NOT NULL,
  idx INTEGER UNSIGNED,
  object VARCHAR(256),
  PRIMARY KEY(id),
  FOREIGN KEY(name_key) REFERENCES playlist.name(id) ON DELETE CASCADE
)  ENGINE INNODB;

CREATE UNIQUE INDEX find_item ON playlist.playlist (name_key,idx);
