CREATE SCHEMA file_locator;

CREATE TABLE file_locator.hosts (
  id SERIAL PRIMARY KEY,
  host VARCHAR(20) NOT NULL
) ENGINE INNODB;

CREATE UNIQUE INDEX by_host ON file_locator.hosts(host);

CREATE TABLE file_locator.protocols (
  id SERIAL PRIMARY KEY,
  protocol VARCHAR(10)
) ENGINE INNODB;

CREATE TABLE file_locator.catalog_ids (
  id SERIAL PRIMARY KEY,
  catalog_id CHAR(12) NOT NULL
) ENGINE INNODB;

CREATE UNIQUE INDEX by_catalog_id ON file_locator.catalog_ids(catalog_id);

CREATE TABLE file_locator.urls (
  id SERIAL PRIMARY KEY,
  host_id BIGINT(20) UNSIGNED NOT NULL,
  protocol_id BIGINT(20) UNSIGNED NOT NULL,
  catalog_id BIGINT(20) UNSIGNED NOT NULL,
  path VARCHAR(256) NOT NULL,
  FOREIGN KEY (host_id) REFERENCES file_locator.hosts(id),
  FOREIGN KEY (protocol_id) REFERENCES file_locator.protocol(id),
  FOREIGN KEY (catalog_id) REFERENCES file_locator.catalog_ids(id)
) ENGINE INNODB;

CREATE UNIQUE INDEX by_host_protocol_catalog ON file_locator.urls(catalog_id,host_id,protocol_id);
