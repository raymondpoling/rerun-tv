CREATE SCHEMA meta;

CREATE TABLE meta.series (
  id SERIAL,
  name VARCHAR(100),
  catalog_prefix CHAR(7) UNIQUE NOT NULL,
  summary TEXT,
  thumbnail VARCHAR(255),
  imdbid CHAR(10)
) ENGINE INNODB;

CREATE UNIQUE INDEX by_series_name ON meta.series(name);

CREATE UNIQUE INDEX by_catalog_prefix ON meta.series(catalog_prefix);

CREATE TABLE meta.season_title (
  id SERIAL,
  series_id BIGINT(20) UNSIGNED NOT NULL,
  season INTEGER UNSIGNED NOT NULL,
  title VARCHAR(50) NOT NULL,
  FOREIGN KEY (series_id) REFERENCES meta.series(id) ON DELETE CASCADE
) ENGINE INNODB;

CREATE UNIQUE INDEX subtitles ON meta.season_title(series_id, season);

CREATE TABLE meta.files (
  id SERIAL,
  -- catalog_id CHAR(12),
  series_id BIGINT(20) UNSIGNED NOT NULL,
  season INTEGER UNSIGNED NOT NULL,
  episode INTEGER UNSIGNED NOT NULL,
  episode_name VARCHAR(120),
  summary TEXT,
  thumbnail VARCHAR(255),
  imdbid CHAR(10),
  FOREIGN KEY (series_id) REFERENCES meta.series(id) ON DELETE CASCADE
) ENGINE INNODB;

-- CREATE UNIQUE INDEX by_catalog_id ON meta.files(catalog_id);

CREATE UNIQUE INDEX by_series_season_episode ON meta.files(series_id,season,episode);
