DROP TABLE user.user;

DROP TABLE user.pointer;

CREATE TABLE user.user (
  id SERIAL,
  username VARCHAR(20),
  PRIMARY KEY (id)
) ENGINE INNODB;

CREATE UNIQUE INDEX by_user ON user.user (username);

CREATE TABLE user.index (
  user_id BIGINT(20) UNSIGNED NOT NULL,
  idx INTEGER UNSIGNED NOT NULL,
  schedule VARCHAR(20) NOT NULL,
  PRIMARY KEY (user_id, schedule),
  FOREIGN KEY (user_id) REFERENCES user.user(id) ON DELETE CASCADE
) ENGINE INNODB;
